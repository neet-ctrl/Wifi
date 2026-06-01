#pragma once
/*
 * Local declaration stub for BoringSSL SPAKE2 API.
 * The boringssl-ndk prefab ships libcrypto_static.a with SPAKE2_* symbols
 * but does NOT export spake2.h in its public headers.
 * This file supplies the declarations so the compiler is satisfied;
 * the linker resolves them from boringssl::crypto_static at link time.
 */

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum spake2_role_t {
    spake2_role_alice = 0,
    spake2_role_bob   = 1,
};

typedef struct spake2_ctx_st SPAKE2_CTX;

#define SPAKE2_MAX_MSG_SIZE 32
#define SPAKE2_MAX_KEY_SIZE 64

SPAKE2_CTX *SPAKE2_CTX_new(enum spake2_role_t role,
                            const uint8_t *my_name,    size_t my_name_len,
                            const uint8_t *their_name, size_t their_name_len);

void SPAKE2_CTX_free(SPAKE2_CTX *ctx);

int SPAKE2_generate_msg(SPAKE2_CTX *ctx,
                        uint8_t *out, size_t *out_len, size_t max_out_len,
                        const uint8_t *password, size_t password_len);

int SPAKE2_process_msg(SPAKE2_CTX *ctx,
                       uint8_t *out_key, size_t *out_key_len, size_t max_out_key_len,
                       const uint8_t *their_msg, size_t their_msg_len);

#ifdef __cplusplus
}
#endif
