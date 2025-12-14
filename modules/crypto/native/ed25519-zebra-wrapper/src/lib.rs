use curve25519_dalek::edwards::CompressedEdwardsY;
use ed25519_zebra::{Signature, SigningKey, VerificationKey, VerificationKeyBytes};
use jni::objects::{JByteArray, JClass};
use jni::sys::jbyteArray;
use jni::JNIEnv;

/// Ed25519 field prime p = 2^255 - 19
const FIELD_PRIME: [u8; 32] = [
    0xed, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f,
];

/// Check if a 32-byte point encoding is canonical (y < p) AND represents a valid curve point
/// with correct sign bit encoding.
///
/// A point encoding is canonical if:
/// 1. The y-coordinate (with sign bit cleared) is strictly less than the field prime p
/// 2. The point lies on the Ed25519 curve
/// 3. The sign bit correctly represents the x-coordinate (no alternate encodings)
fn is_canonical_point(point: &[u8; 32]) -> bool {
    // Check y < p (with sign bit cleared)
    let mut y_cleared = *point;
    y_cleared[31] &= 0x7f; // Clear sign bit

    // Compare y against field prime p (little-endian, most significant byte first)
    for i in (0..32).rev() {
        if y_cleared[i] < FIELD_PRIME[i] {
            // y < p at this byte position, canonical so far
            break;
        }
        if y_cleared[i] > FIELD_PRIME[i] {
            // y > p, not canonical
            return false;
        }
        // y[i] == FIELD_PRIME[i], continue checking lower bytes
        if i == 0 {
            // y == p, not canonical (must be strictly less)
            return false;
        }
    }

    // Check if the point is on the curve using curve25519-dalek
    let compressed = CompressedEdwardsY(*point);
    let decompressed = match compressed.decompress() {
        Some(p) => p,
        None => return false,
    };

    // Check for non-canonical sign bit encoding
    // When x = 0, there's only one valid encoding (sign bit = 0)
    // When x != 0, the sign bit must match the actual x coordinate's sign
    // Re-compress the point and verify it matches the original encoding
    let recompressed = decompressed.compress();
    recompressed.as_bytes() == point
}

#[no_mangle]
pub extern "system" fn Java_io_forge_jam_crypto_Ed25519ZebraWrapper_verify(
    mut env: JNIEnv,
    _class: JClass,
    public_key: JByteArray,
    message: JByteArray,
    signature: JByteArray,
) -> jbyteArray {
    let result = verify_impl(&mut env, public_key, message, signature);

    let result_byte: [u8; 1] = if result { [1] } else { [0] };

    match env.byte_array_from_slice(&result_byte) {
        Ok(array) => array.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn verify_impl(
    env: &mut JNIEnv,
    public_key: JByteArray,
    message: JByteArray,
    signature: JByteArray,
) -> bool {
    // Convert public key bytes
    let pk_bytes = match env.convert_byte_array(&public_key) {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };

    if pk_bytes.len() != 32 {
        return false;
    }

    // Convert to fixed-size array
    let pk_array: [u8; 32] = match pk_bytes.try_into() {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };

    // Check public key canonicity (y < p AND valid curve point)
    if !is_canonical_point(&pk_array) {
        return false;
    }

    // Convert signature bytes
    let sig_bytes = match env.convert_byte_array(&signature) {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };

    if sig_bytes.len() != 64 {
        return false;
    }

    // Extract R value (first 32 bytes of signature) and check canonicity
    let r_array: [u8; 32] = match sig_bytes[0..32].try_into() {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };

    if !is_canonical_point(&r_array) {
        return false;
    }

    // Convert message bytes
    let msg_bytes = match env.convert_byte_array(&message) {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };

    // Create verification key from bytes
    let vk_bytes = VerificationKeyBytes::from(pk_array);
    let vk: VerificationKey = match vk_bytes.try_into() {
        Ok(vk) => vk,
        Err(_) => return false,
    };

    // Create signature
    let sig_array: [u8; 64] = match sig_bytes.try_into() {
        Ok(bytes) => bytes,
        Err(_) => return false,
    };
    let sig = Signature::from(sig_array);

    // Verify using ed25519-zebra
    vk.verify(&sig, &msg_bytes).is_ok()
}

/// Sign a message using Ed25519.
///
/// # Arguments
/// * `secret_key` - 32-byte Ed25519 secret key seed
/// * `message` - The message to sign
///
/// # Returns
/// * 64-byte signature on success
/// * null on failure
#[no_mangle]
pub extern "system" fn Java_io_forge_jam_crypto_Ed25519ZebraWrapper_sign(
    mut env: JNIEnv,
    _class: JClass,
    secret_key: JByteArray,
    message: JByteArray,
) -> jbyteArray {
    let return_null = || -> jbyteArray { std::ptr::null_mut() };

    // Convert secret key bytes
    let sk_bytes = match env.convert_byte_array(&secret_key) {
        Ok(bytes) => bytes,
        Err(_) => return return_null(),
    };

    if sk_bytes.len() != 32 {
        return return_null();
    }

    // Convert message bytes
    let msg_bytes = match env.convert_byte_array(&message) {
        Ok(bytes) => bytes,
        Err(_) => return return_null(),
    };

    // Create signing key from seed
    let sk_array: [u8; 32] = match sk_bytes.try_into() {
        Ok(bytes) => bytes,
        Err(_) => return return_null(),
    };
    let signing_key = SigningKey::from(sk_array);

    // Sign the message
    let signature = signing_key.sign(&msg_bytes);
    let sig_bytes: [u8; 64] = signature.into();

    match env.byte_array_from_slice(&sig_bytes) {
        Ok(array) => array.into_raw(),
        Err(_) => return_null(),
    }
}

/// Get the public key from a secret key.
///
/// # Arguments
/// * `secret_key` - 32-byte Ed25519 secret key seed
///
/// # Returns
/// * 32-byte public key on success
/// * null on failure
#[no_mangle]
pub extern "system" fn Java_io_forge_jam_crypto_Ed25519ZebraWrapper_publicFromSecret(
    mut env: JNIEnv,
    _class: JClass,
    secret_key: JByteArray,
) -> jbyteArray {
    let return_null = || -> jbyteArray { std::ptr::null_mut() };

    // Convert secret key bytes
    let sk_bytes = match env.convert_byte_array(&secret_key) {
        Ok(bytes) => bytes,
        Err(_) => return return_null(),
    };

    if sk_bytes.len() != 32 {
        return return_null();
    }

    // Create signing key from seed
    let sk_array: [u8; 32] = match sk_bytes.try_into() {
        Ok(bytes) => bytes,
        Err(_) => return return_null(),
    };
    let signing_key = SigningKey::from(sk_array);

    // Get public key
    let vk = VerificationKey::from(&signing_key);
    let vk_bytes: [u8; 32] = vk.into();

    match env.byte_array_from_slice(&vk_bytes) {
        Ok(array) => array.into_raw(),
        Err(_) => return_null(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sign_and_verify() {
        // Create a signing key
        let seed = [1u8; 32];
        let signing_key = SigningKey::from(seed);
        let vk = VerificationKey::from(&signing_key);

        // Sign a message
        let message = b"test message";
        let signature = signing_key.sign(message);

        // Verify the signature
        assert!(vk.verify(&signature, message).is_ok());
    }

    #[test]
    fn test_canonical_point_check() {
        // Test non-canonical-0: y=1 with sign bit (not a valid curve point)
        let non_canonical_0 =
            hex::decode("0100000000000000000000000000000000000000000000000000000000000080")
                .unwrap();
        let arr: [u8; 32] = non_canonical_0.try_into().unwrap();
        assert!(
            !is_canonical_point(&arr),
            "non-canonical-0 should be rejected"
        );

        // Test non-canonical-1: y=p-1 with sign bit (not a valid curve point)
        let non_canonical_1 =
            hex::decode("ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                .unwrap();
        let arr: [u8; 32] = non_canonical_1.try_into().unwrap();
        assert!(
            !is_canonical_point(&arr),
            "non-canonical-1 should be rejected"
        );

        // Test non-canonical-2: y=p (not canonical, y >= p)
        let non_canonical_2 =
            hex::decode("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f")
                .unwrap();
        let arr: [u8; 32] = non_canonical_2.try_into().unwrap();
        assert!(
            !is_canonical_point(&arr),
            "non-canonical-2 should be rejected"
        );

        // Test canonical-0: y=1 without sign bit (identity point, valid)
        let canonical_0 =
            hex::decode("0100000000000000000000000000000000000000000000000000000000000000")
                .unwrap();
        let arr: [u8; 32] = canonical_0.try_into().unwrap();
        assert!(is_canonical_point(&arr), "canonical-0 should be accepted");

        // Test canonical-1: a real canonical public key
        let canonical_1 =
            hex::decode("c7176a703d4dd84fba3c0b760d10670f2a2053fa2c39ccc64ec7fd7792ac037a")
                .unwrap();
        let arr: [u8; 32] = canonical_1.try_into().unwrap();
        assert!(is_canonical_point(&arr), "canonical-1 should be accepted");
    }
}
