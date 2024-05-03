package com.crypho.plugins;

import java.io.File;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.security.keystore.UserNotAuthenticatedException;
import android.telecom.Call;
import android.util.Log;
import android.util.Base64;
import android.os.Build;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.outsystems.plugins.keystore.controller.KeystoreController;
import com.outsystems.plugins.keystore.controller.KeystoreError;
import com.outsystems.plugins.keystore.controller.OSKSTRBiometricPromptInfo;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.crypto.IllegalBlockSizeException;

public class SecureStorage extends CordovaPlugin {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final String TAG = "SecureStorage";

    private static final boolean SUPPORTS_NATIVE_AES = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final boolean SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private static final String MSG_NOT_SUPPORTED = "API 19 (Android 4.4 KitKat) is required. This device is running API " + Build.VERSION.SDK_INT;
    private static final String MSG_DEVICE_NOT_SECURE = "Device is not secure";
    private static final String MSG_AUTH_SKIPPED = "Authentication screen skipped";
    public static final String MIGRATED_FOR_SECURITY = "_SS_MIGRATED_FOR_SECURITY";

    private static final String MIGRATED_FOR_ENCRYPTED = "MIGRATED_FOR_ENCRYPTED";
    private static final String MSG_USER_NOT_AUTHENTICATED = "User not authenticated";
    private static final String ERROR_FORMAT_PREFIX = "OS-PLUG-KSTR-";
    private static final String MIGRATION_AUTH = "migration_auth";
    private static final String CIPHERED_KEY = "_SS_outsystems-local-storage-key";

    private KeystoreController keystoreController = null;
    private CallbackContext callbackContext = null;

    private String currentStore = "outsystems-key-store";

    private final Hashtable<String, SharedPreferencesHandler> SERVICE_STORAGE = new Hashtable<String, SharedPreferencesHandler>();

    private IntentRequestQueue intentRequestQueue;

    @Override
    protected void pluginInitialize() {

        super.pluginInitialize();

        intentRequestQueue = new IntentRequestQueue(this);

        keystoreController = new KeystoreController(
                new OSKSTRBiometricPromptInfo(
                        getStringResource(this.cordova.getActivity(), "biometric_prompt_title"),
                        getStringResource(this.cordova.getActivity(), "biometric_prompt_subtitle"),
                        getStringResource(this.cordova.getActivity(), "biometric_prompt_negative_button")
                )
        );
    }


    private void securityMigration(CallbackContext callbackContext) throws JSONException {
        //transfer all existing items to new table
        Hashtable<Integer, TransitionValue> transitionTable = new Hashtable<Integer, TransitionValue>();
        Hashtable<String,Boolean> RSAMap= new Hashtable<String, Boolean>();
        Enumeration<String> services = SERVICE_STORAGE.keys();
        while(services.hasMoreElements()){
            String service = services.nextElement();
            //initializing rsakeymapper
            RSAMap.put(service, false);
            SharedPreferencesHandler handler = SERVICE_STORAGE.get(service);
            Set<String> keys = handler.keys();

            for(String key : keys){
                String value = handler.fetch(key);
                ExecutorResult result = decryptHelper(value, service,callbackContext);

                if(result.type != ExecutorResultType.ERROR){
                    TransitionValue t = new TransitionValue(service, key, result.result);
                    SecureRandom i = new SecureRandom();
                    transitionTable.put(i.nextInt(), t);
                }
                else{
                    callbackContext.error("MIGRATION FAILED : " + result.result);
                    return;
                }
            }
        }

        //Reinsert data with new keys
        Enumeration<Integer> transitionKeys = transitionTable.keys();
        
        while(transitionKeys.hasMoreElements()){
            Integer key = transitionKeys.nextElement();
            TransitionValue tv = transitionTable.get(key);

            //RSA key needs to be created for each service
            if(!RSAMap.get(tv.getService())){
                try{
                    RSA.createKeyPair(getContext(),service2alias(tv.getService()));

                    RSAMap.put(tv.getService(), true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //the encryptor helper already inserts items into storage
            ExecutorResult result = encrytionHelper(tv.getService(),tv.getKey(), tv.getValue());
            if(result.type == ExecutorResultType.ERROR){
                callbackContext.error("MIGRATION FAILED : " + result.result);
                return;
            }
        }
        

        Context ctx = getContext();
        SharedPreferences preferences = ctx.getSharedPreferences(ctx.getPackageName() + "_SM", 0);
        markAsMigrated(preferences);
        

    }

    private boolean isDeviceSecure() {
        KeyguardManager keyguardManager = (KeyguardManager) (getContext().getSystemService(Context.KEYGUARD_SERVICE));
        try {

            // This tries to call isDeviceSecure, which was only added in API 23
            // The method checks if there is a lock screen that requires authentication defined (not None or Swipe)
            // This is preferred to the older method isKeyguardSecure, that also returns true if the SIM card is unlocked
            Method isSecure = null;
            isSecure = keyguardManager.getClass().getMethod("isDeviceSecure");
            return ((Boolean) isSecure.invoke(keyguardManager)).booleanValue();

        } catch (Exception e) {

            // Best effort if the preferred method is unavailable
            return keyguardManager.isKeyguardSecure();
        }
    }

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {

        if (!SUPPORTED) {
            Log.w(TAG, MSG_NOT_SUPPORTED);
            callbackContext.error(MSG_NOT_SUPPORTED);
            return false;
        }

        Log.v("ACTION: ", action);

        boolean result = false;
        switch (action) {
            case "init":
                result = init(args, callbackContext);
                break;
            case "set":
                result = set(args, callbackContext);
                break;
            case "get":
                result = get(args, callbackContext);
                break;
            case "decrypt_rsa":
                result = decrypt_rsa(args, callbackContext);
                break;
            case "encrypt_rsa":
                result = encrypt_rsa(args, callbackContext);
                break;
            case "secureDevice":
                result = secureDevice(callbackContext);
                break;
            case "remove":
                result = remove(args, callbackContext);
                break;
            case "store":
                result = store(args, callbackContext);
                break;
            case "fetch":
                result = fetch(args, callbackContext);
                break;
            case "keys":
                result = keys(args, callbackContext);
                break;
            case "clear":
                result = clear(args, callbackContext);
        }


        return result;
    }

    // Called when a SecureStorage Javascript object is created
    // Returns an error if a lock screen that requires authentication is not defined
    // Creates a private key for an alias based on the name of the store (if it does not exist already)
    private boolean init(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called init action");


        // Get key alias based on the name of the store
        String service = args.getString(0);
        String alias = service2alias(service);

        // Create helper object to manage a SharedPreferences object for the alias
        SharedPreferencesHandler PREFS = new SharedPreferencesHandler(alias + "_SS", getContext());
        putStorage(service, PREFS);

        if(checkForSecurityMigration()){

            try {
                securityMigration(callbackContext);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(isMigrationToEncryptedNeeded()){
            Boolean migrationSuccessful = doDataMigration(callbackContext);
            if(migrationSuccessful){
                callbackContext.success(1);
            }
        }
        else{
            callbackContext.success(1);
        }

        return true;
    }

    private Boolean doDataMigration(CallbackContext callbackContext){

        this.callbackContext = callbackContext;
        Boolean authFromResources = Boolean.parseBoolean(cordova.getActivity().getString(this.getBooleanResourceId(cordova.getActivity(), MIGRATION_AUTH)));

        try {
            Enumeration<String> services = SERVICE_STORAGE.keys();
            while(services.hasMoreElements()){
                String service = services.nextElement();
                SharedPreferencesHandler handler = SERVICE_STORAGE.get(service);
                Set<String> keys = handler.keys();

                for(String key : keys){
                    String value = handler.fetch(key);
                    ExecutorResult result = decryptHelper(value, service, callbackContext);

                    Boolean toAuthenticate = false;
                    if(!key.equals(CIPHERED_KEY)){
                        toAuthenticate = authFromResources;
                    }

                    if(result.type != ExecutorResultType.ERROR){
                        keystoreController.setValues(
                                key,
                                result.result,
                                service,
                                toAuthenticate
                        );
                        keystoreController.setValueEncrypted(cordova.getActivity());
                        handler.remove(key);
                    }
                    else{
                        if(result.result.equals(MSG_USER_NOT_AUTHENTICATED)){
                            //in this case, we should request user authentication and try proceeding with the migration
                            cordova.setActivityResultCallback(this);
                            keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_MIGRATION);
                        }
                        else{
                            callbackContext.error("MIGRATION FAILED : " + result.result);
                        }
                        return false;
                    }
                }
                SERVICE_STORAGE.remove(service);
            }
            markAsMigratedToEncrypted();
            return true;
        } catch (JSONException e){
            Log.d(TAG, e.getMessage());
            callbackContext.error("MIGRATION FAILED");
            return false;
        } catch (Exception e){
            if(e.getCause() instanceof UserNotAuthenticatedException){
                cordova.setActivityResultCallback(this);
                keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_MIGRATION);
            }
            else{
                Log.d(TAG, e.getMessage());
                callbackContext.error("MIGRATION FAILED");
            }
            return false;
        }
    }

    private Boolean isMigrationToEncryptedNeeded(){
        return !cordova.getActivity().getSharedPreferences(MIGRATED_FOR_ENCRYPTED, Context.MODE_PRIVATE).getBoolean(MIGRATED_FOR_ENCRYPTED, false);
    }

    private void markAsMigratedToEncrypted(){
        cordova.getActivity().getSharedPreferences(MIGRATED_FOR_ENCRYPTED, Context.MODE_PRIVATE).edit().putBoolean(MIGRATED_FOR_ENCRYPTED, true).apply();
    }

    private boolean checkForSecurityMigration() {
        Context ctx = getContext();
        SharedPreferences preferences = ctx.getSharedPreferences(ctx.getPackageName() + "_SM",0);
        String isMigrated = preferences.getString(MIGRATED_FOR_SECURITY, "");
        //check OS then check if keys exist and migration was done
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

            int size = initializePreferences();
            if(isMigrated.equals("TRUE")){
                return false;
            }
            //the target case of migration
            else if(size > 0){
                return true;
            }
            //new use, meaning we should put the tag in as to not trigger a unwanted migration
            else{
                markAsMigrated(preferences);
                return false;
            }
        }
        else{
            //nothing to do return false
            return false;
        }
    }

    private void markAsMigrated(SharedPreferences preferences) {

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(MIGRATED_FOR_SECURITY, "TRUE");
        editor.commit();
    }

   private int initializePreferences() {

        Context ctx = getContext();
        File prefdir = new File(ctx.getApplicationInfo().dataDir,"shared_prefs");
        String[] filenames = prefdir.list();
        int i = 0;
        for(String name : filenames){
            String packageName = ctx.getPackageName();
            if(name.startsWith(ctx.getPackageName()) && name.endsWith("_SS.xml")){
                String alias = name.substring(0, name.length() - 4);
                String service = alias.substring(ctx.getPackageName().length() + 1, alias.length() -3);

                SharedPreferencesHandler PREFS = new SharedPreferencesHandler(alias, getContext());
                putStorage(service, PREFS);
                i+=1;


            }
        }
        return i;
    }

    // Store a key/enc-value pair in SharedPreferences
    // The encryption uses the key with an alias associated with the store name
    private boolean set(CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        Log.v(TAG, "Called set action");

        this.callbackContext = callbackContext;

        final String store = args.getString(0);
        final String key = args.getString(1);
        final String value = args.getString(2);
        final Boolean authenticate = args.getBoolean(3);

        keystoreController.setValues(key, value, store, authenticate);
        if(authenticate){
            cordova.setActivityResultCallback(this);
            keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_SET);
        }
        else{
            try{
                keystoreController.setValueEncrypted(cordova.getActivity());
                this.callbackContext.success();
            }
            catch (Exception e){
                Log.d(TAG, e.getMessage());
                sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
            }
        }
        return true;
    }

    private ExecutorResult encrytionHelper(String service, String key, String value) {

        ExecutorResult result;

        EncryptionExecutor encryptionExecutor = new EncryptionExecutor(service, key, value, cordova.getContext());
        Future<ExecutorResult> exec = cordova.getThreadPool().submit(encryptionExecutor);

        try{
            result = exec.get();
            if(result.type == ExecutorResultType.SUCCESS){
                getStorage(service).store(key, result.result);
            }
        } catch (InterruptedException e) {
            result = new ExecutorResult(ExecutorResultType.ERROR,e.getMessage());
        } catch (ExecutionException e) {
            result = new ExecutorResult(ExecutorResultType.ERROR,e.getMessage());
        }
        return result;

    }

    // Get the enc-value associated with a key in SharedPreferences
    // The decryption uses the key with an alias associated with the store name
    private boolean get(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called get action");

        this.callbackContext = callbackContext;

        final String store = args.getString(0);
        final String key = args.getString(1);

        if(!cordova.getActivity().getSharedPreferences(store + key, Context.MODE_PRIVATE).contains(store + key)){
            sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
        }
        else{
            Boolean authenticate = cordova.getActivity().getSharedPreferences(store + key, Context.MODE_PRIVATE).getBoolean(store + key, false);
            keystoreController.setValues(key, null, store, authenticate);
            if(authenticate){
                cordova.setActivityResultCallback(this);
                keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_GET);
            }
            else{
                try{
                    String value = keystoreController.getValueEncrypted(cordova.getActivity());
                    if(value != null){
                        callbackContext.success(value);
                    }
                    else{
                        sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
                    }
                }
                catch (Exception e){
                    Log.d(TAG, e.getMessage());
                    sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
                }
            }
        }
        return true;
    }


    private ExecutorResult decryptHelper(String value, String service, CallbackContext callbackContext) throws JSONException {
        JSONObject json = new JSONObject(value);
        final byte[] encKey = Base64.decode(json.getString("key"), Base64.DEFAULT);
        JSONObject data = json.getJSONObject("value");
        final byte[] ct = Base64.decode(data.getString("ct"), Base64.DEFAULT);
        final byte[] iv = Base64.decode(data.getString("iv"), Base64.DEFAULT);
        final byte[] adata = Base64.decode(data.getString("adata"), Base64.DEFAULT);



        DecryptionExecutor decryptionExecutor = new DecryptionExecutor(encKey, service2alias(service), iv, ct, adata);
        Future<ExecutorResult> decryptThread = cordova.getThreadPool().submit(decryptionExecutor);

        //thread blocks here until result
        ExecutorResult decrypted;
        try {
            decrypted = decryptThread.get();
        } catch (InterruptedException e) {
            decrypted = new ExecutorResult(ExecutorResultType.ERROR, e.getMessage());
        } catch (ExecutionException e) {
            decrypted = new ExecutorResult(ExecutorResultType.ERROR, e.getMessage());
        }

        return decrypted;

    }

    // Decrypt a message using the key with an alias associated with the store name
    private boolean decrypt_rsa(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called decrypt_rsa action");
        final String service = args.getString(0);
        // getArrayBuffer does base64 decoding
        final byte[] decryptMe = args.getArrayBuffer(1);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    byte[] decrypted = RSA.decrypt(decryptMe, service2alias(service));
                    callbackContext.success(new String(decrypted));
                } catch (Exception e) {
                    Log.e(TAG, "Decrypt (RSA) failed :", e);
                    callbackContext.error(e.getMessage());
                }
            }
        });
        return true;
    }

    // Encrypt a message using the key with an alias associated with the store name
    private boolean encrypt_rsa(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called encrypt_rsa action");
        final String service = args.getString(0);
        final String encryptMe = args.getString(1);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    byte[] encrypted = RSA.encrypt(encryptMe.getBytes(), service2alias(service));
                    callbackContext.success(Base64.encodeToString(encrypted, Base64.DEFAULT));
                } catch (Exception e) {
                    Log.e(TAG, "Encrypt (RSA) failed :", e);
                    callbackContext.error(e.getMessage());
                }
            }
        });
        return true;
    }

    // Check if there is a lock screen that requires authentication defined
    // It gives the user the possibility of defining one if there isn't one
    // Used by the Ciphered Local Storage Plugin at startup
    private boolean secureDevice(CallbackContext callbackContext) {
        Log.v(TAG, "Called secureDevice action");
        handleLockScreen(IntentRequestType.SECURE_DEVICE, null, callbackContext);
        return true;
    }

    // The remaining actions are the SharedPreferences interface
    private boolean remove(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called remove action");

        this.callbackContext = callbackContext;

        String store = args.getString(0);
        String key = args.getString(1);

        if(!cordova.getActivity().getSharedPreferences(store + key, Context.MODE_PRIVATE).contains(store + key)){
            sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
        }
        else{
            Boolean authenticate = cordova.getActivity().getSharedPreferences(store + key, Context.MODE_PRIVATE).getBoolean(store + key, false);
            keystoreController.setValues(key, null, store, authenticate);

            if(authenticate){
                cordova.setActivityResultCallback(this);
                keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_REMOVE);
            }
            else{
                try{
                    Boolean removed = keystoreController.removeKeyEncrypted(cordova.getActivity());
                    if(removed){
                        callbackContext.success();
                    }
                    else{
                        sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
                    }
                }
                catch (Exception e){
                    Log.d(TAG, e.getMessage());
                    sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
                }
            }
        }
        return true;
    }

    private boolean store(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called store action");
        String service = args.getString(0);
        String key = args.getString(1);
        String value = args.getString(2);
        getStorage(service).store(key, value);
        callbackContext.success();
        return true;
    }

    private boolean fetch(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called fetch action");
        callbackContext.success();
        return true;
    }

    private boolean keys(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called keys action");
        String store = args.getString(0);
        this.currentStore = store;
        this.callbackContext = callbackContext;
        try{
            getKeys(store);
        }
        catch (Exception e){
            if(e.getCause() instanceof UserNotAuthenticatedException){
                cordova.setActivityResultCallback(this);
                keystoreController.showBiometricPrompt(cordova.getActivity(), KeystoreController.REQUEST_CODE_BIOMETRIC_KEYS);
            }
            else{
                Log.d(TAG, e.getMessage());
                callbackContext.error(e.getMessage());
            }
        }
        return true;
    }

    private void getKeys(String store){
        this.keystoreController.setValues(null, null, store, false);
        this.callbackContext.success(new JSONArray(keystoreController.getEncryptedStoreKeys(cordova.getActivity())));
    }

    private boolean clear(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Called clear action");
        String service = args.getString(0);
        getStorage(service).clear();
        callbackContext.success();
        return true;
    }

    private String service2alias(String service) {
        return getContext().getPackageName() + "." + service;
    }

    private SharedPreferencesHandler getStorage(String service) {
        synchronized (SERVICE_STORAGE) {
            return SERVICE_STORAGE.get(service);
        }
    }

    private void putStorage(String service, SharedPreferencesHandler handler) {
        synchronized (SERVICE_STORAGE) {
            SERVICE_STORAGE.put(service, handler);
        }
    }

    private void initSuccess(CallbackContext context) {
        // 0 is falsy in js while 1 is truthy
        context.success(SUPPORTS_NATIVE_AES ? 1 : 0);
    }

    private void handleLockScreen(final IntentRequestType type, final String service, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Log.v(TAG, "Handling lock screen");

                // fix applied in context of RMET-1182
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    handleLockScreenUsingNoOpOrSetNewPasswordIntent(type, service, callbackContext);
                } else {
                    handleLockScreenUsingUnlockIntent(type, service, callbackContext);
                }
            }
        });
    }

    // Made in context of RNMT-3255, RNMT-3540 and RNMT-3803
    private void handleLockScreenUsingNoOpOrSetNewPasswordIntent(IntentRequestType type, String service, CallbackContext callbackContext) {
        Log.v(TAG, "Handling lock screen via KeyguardManager or ACTION_SET_NEW_PASSWORD intent (Android 6 or newer)");

        if (isDeviceSecure()) {
            Log.v(TAG, "Unlocking Android devices above 6 using KeyguardManager");
            KeyguardManager keyguardManager = (KeyguardManager) (getContext().getSystemService(Context.KEYGUARD_SERVICE));
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            // Lock screen is already defined, unlock it via KeyguardManager
            intentRequestQueue.queueRequest(type, service, intent, callbackContext);

        } else {
            Log.v(TAG, "Lock screen is not defined, requesting one via ACTION_SET_NEW_PASSWORD intent");

            // Lock screen is not defined, so we request a new one
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            intentRequestQueue.queueRequest(type, service, intent, callbackContext);
        }
    }

    private void handleLockScreenUsingUnlockIntent(IntentRequestType type, String service, CallbackContext callbackContext) {
        Log.v(TAG, "Handling lock screen via UNLOCK intent (Android 6 or earlier)");

        // Requests a new lock screen or requests to unlock if required
        Intent intent = new Intent("com.android.credentials.UNLOCK");
        intentRequestQueue.queueRequest(type, service, intent, callbackContext);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(TAG, "Activity started by intent has finished");

        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == KeystoreController.REQUEST_CODE_BIOMETRIC_SET){

            switch (resultCode){

                case Activity.RESULT_OK:
                    try{
                        keystoreController.setValueEncrypted(cordova.getActivity());
                        this.callbackContext.success();
                    }
                    catch (Exception e){
                        Log.d(TAG, e.getMessage());
                        sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
                    }
                    break;

                case KeystoreController.RESULT_DEVICE_NOT_SECURE:
                    sendError(KeystoreError.DEVICE_NOT_SECURE);
                    break;

                case Activity.RESULT_CANCELED:
                    sendError(KeystoreError.AUTHENTICATION_FAILED_ERROR);

                default:
                    break;
            }
        }
        else if(requestCode == KeystoreController.REQUEST_CODE_BIOMETRIC_GET){
            switch (resultCode){

                case Activity.RESULT_OK:
                    try{
                        String value = keystoreController.getValueEncrypted(cordova.getActivity());
                        if(value != null){
                            this.callbackContext.success(value);
                        }
                        else{
                            sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
                        }
                    }
                    catch (Exception e){
                        Log.d(TAG, e.getMessage());
                        sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
                    }
                    break;

                case KeystoreController.RESULT_DEVICE_NOT_SECURE:
                    sendError(KeystoreError.DEVICE_NOT_SECURE);
                    break;

                case Activity.RESULT_CANCELED:
                    sendError(KeystoreError.AUTHENTICATION_FAILED_ERROR);

                default:
                    break;
            }
        }
        else if(requestCode == KeystoreController.REQUEST_CODE_BIOMETRIC_REMOVE){
            switch (resultCode){

                case Activity.RESULT_OK:
                    try{
                        Boolean removed = keystoreController.removeKeyEncrypted(cordova.getActivity());
                        if(removed){
                            this.callbackContext.success();
                        }
                        else{
                            sendError(KeystoreError.KEY_NOT_FOUND_ERROR);
                        }
                    }
                    catch (Exception e){
                        Log.d(TAG, e.getMessage());
                        sendError(KeystoreError.KEYSTORE_GENERAL_ERROR);
                    }
                    break;

                case KeystoreController.RESULT_DEVICE_NOT_SECURE:
                    sendError(KeystoreError.DEVICE_NOT_SECURE);
                    break;

                case Activity.RESULT_CANCELED:
                    sendError(KeystoreError.AUTHENTICATION_FAILED_ERROR);

                default:
                    break;
            }
        }
        else if(requestCode == KeystoreController.REQUEST_CODE_BIOMETRIC_MIGRATION){
            switch (resultCode){

                case Activity.RESULT_OK:
                    Boolean result = doDataMigration(callbackContext);
                    if(result){
                        this.callbackContext.success(1);
                    }
                    else{
                        sendError(KeystoreError.AUTHENTICATION_FAILED_ERROR);
                    }
                    break;

                case KeystoreController.RESULT_DEVICE_NOT_SECURE:
                    sendError(KeystoreError.DEVICE_NOT_SECURE);
                    break;

                case Activity.RESULT_CANCELED:
                    callbackContext.error("MIGRATION FAILED");

                default:
                    break;
            }
        }
        else if(requestCode == KeystoreController.REQUEST_CODE_BIOMETRIC_KEYS){
            switch (resultCode){

                case Activity.RESULT_OK:
                    getKeys(currentStore);
                    break;

                case Activity.RESULT_CANCELED:
                    callbackContext.error(MSG_AUTH_SKIPPED);

                default:
                    break;
            }
        }
        //old code below
        else {
            IntentRequest request = intentRequestQueue.notifyActivityResultCalled();

            String service = request.getService();
            CallbackContext callbackContext = request.getCallbackContext();

            // when the user clicks in the back button, the resultCode is 0
            // when the user authenticate correctly, the resultCode is -1
            IntentRequestType type = request.getType();
            if (resultCode == 0) {
                type = IntentRequestType.AUTHENTICATION_SKIPPED;
            }

            handleCompletedRequest(type, service, callbackContext);
        }
    }

    private void handleCompletedRequest(IntentRequestType type, String service, CallbackContext callbackContext) {
        Log.v(TAG, "Request has completed (maybe from an intent)");

        switch (type) {

            case INIT:
                handleCompletedInit(service, callbackContext);
                break;

            case SECURE_DEVICE:
                handleCompletedSecureDevice(callbackContext);
                break;

            case AUTHENTICATION_SKIPPED:
                handleAuthenticationSkipped(callbackContext);
                break;

            default:
                Log.w(TAG, "Request completion was not handled");
                break;
        }
    }



    private void handleCompletedInit(final String service, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {

                    // RSA already has mutual exclusion in all its public methods individually
                    // But this block requires mutual exclusion as a whole
                    synchronized (SecureStorage.this) {
                        Log.v(TAG, "Completed request is of init action");

                        String alias = service2alias(service);

                        try {
                            if (!RSA.isEntryAvailable(alias)) {
                                //Solves Issue #96. The RSA key may have been deleted by changing the lock type.
                                getStorage(service).clear();
                                RSA.createKeyPair(getContext(), alias);
                            }
                        } catch (UserNotAuthenticatedException e) {
                            Log.v(TAG, "Authentication validity expired, request a new login.");
                        }

                    }

                    Log.v(TAG, "init returned success");
                    initSuccess(callbackContext);

                } catch (Exception e) {
                    Log.e(TAG, "Init returned error because: ", e);
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void handleAuthenticationSkipped(CallbackContext callbackContext) {
        Log.v(TAG, "Completed request with error if the user skipp the Authentication screen");
        callbackContext.error(MSG_AUTH_SKIPPED);
    }

    private void handleCompletedSecureDevice(CallbackContext callbackContext) {
        Log.v(TAG, "Completed request is of secureDevice action");

        if (isDeviceSecure()) {
            Log.v(TAG, "secureDevice returned success");
            callbackContext.success();

        } else {
            Log.v(TAG, "secureDevice returned error");
            callbackContext.error(MSG_DEVICE_NOT_SECURE);
        }
    }


    private Context getContext() {
        return cordova.getActivity().getApplicationContext();
    }

    private void sendError(KeystoreError error){
        JSONObject jsonResult = new JSONObject();
        try{
            jsonResult.put("code", formatErrorCode(error.getCode()));
            jsonResult.put("message", error.getDescription());
            this.callbackContext.error(jsonResult);
        }catch (JSONException e){
            Log.d(TAG, "Error: JSONException occurred while preparing to send an error.");
            this.callbackContext.error("There was an error performing the operation.");
        }
    }

    private String formatErrorCode(int code) {
        String stringCode = Integer.toString(code);
        return ERROR_FORMAT_PREFIX + ("0000" + stringCode).substring(stringCode.length());
    }

    private int getBooleanResourceId(Activity activity, String name) {
        return activity.getResources().getIdentifier(name, "bool", activity.getPackageName());
    }

    private int getStringResourceId(Activity activity, String name) {
        return activity.getResources().getIdentifier(name, "string", activity.getPackageName());
    }

    private String getStringResource(Activity activity, String name) {
        return activity.getString(getStringResourceId(activity, name));
    }
   
}
