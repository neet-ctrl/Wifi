#pragma once
/*
 * Local declaration stub for BoringSSL HKDF API.
 * The boringssl-ndk prefab ships libcrypto_static.a with the HKDF symbol
 * but does NOT export hkdf.h in its public headers.
 * This file supplies the declaration; the linker resolves it from
 * boringssl::crypto_static at link time.
 */

#include <stddef.h>
#include <stdint.h>
#include <openssl/evp.h>

#ifdef __cplusplus
extern "C" {
#endif

int HKDF(uint8_t *out_key, size_t out_len,
         const EVP_MD *digest,
         const uint8_t *secret, size_t secret_len,
         const uint8_t *salt,   size_t salt_len,
         const uint8_t *info,   size_t info_len);

int HKDF_extract(uint8_t *out_key, size_t *out_len,
                 const EVP_MD *digest,
                 const uint8_t *secret, size_t secret_len,
                 const uint8_t *salt,   size_t salt_len);

int HKDF_expand(uint8_t *out_key, size_t out_len,
                const EVP_MD *digest,
                const uint8_t *prk, size_t prk_len,
                const uint8_t *info, size_t info_len);

#ifdef __cplusplus
}
#endif
