package com.crypho.plugins;

import android.util.Log;
import java.util.concurrent.Callable;


public class DecryptionExecutor implements Callable<ExecutorResult> {
        private static final String TAG = "SecureStorage";
        private String alias;
        private byte[] encKey, iv, ct, adata;

        public DecryptionExecutor(byte[] encKey, String alias, byte[] iv, byte[] ct, byte[] adata) {
            this.encKey = encKey;
            this.alias = alias;
            this.iv = iv;
            this.ct = ct;
            this.adata = adata;
        }

        @Override
        public ExecutorResult call() {
            try {
                String decrypted = decrypt();
                return new ExecutorResult(ExecutorResultType.SUCCESS, decrypted);
            } catch (Exception e) {
                Log.e(TAG, "Decrypt (RSA/AES) failed :", e);

                return new ExecutorResult(ExecutorResultType.ERROR, e.getMessage());
            }
        }

        private String decrypt() throws Exception {
            byte[] decryptedKey = RSA.decrypt(encKey, alias);
            String decrypted = new String(AES.decrypt(ct, decryptedKey, iv, adata));
            return decrypted;
        }
    }