# 1.0.0 (2025-12-05)


### Bug Fixes

* add BiometricPrompt and ACTION_SET_NEW_PASSWORD ([d2a348f](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/d2a348ff1f3b7b198871f8c3689daa9ac58c8fe7))
* add more logging ([b4f1c7f](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/b4f1c7f98e59aff29263af61e01284a6f767e7a4))
* added missing exceptions to isEntryAvailable method ([898ffb6](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/898ffb64fd5155120dd64041b2cd3a83d4ae9f19))
* **android:** missing strings.xml on target project ([#41](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/issues/41)) ([0dff642](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/0dff642512ade90a28a653dae6f6df9970337d78))
* **android:** remove unnecessary `kotlin-kapt`, fixing Capacitor builds ([#40](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/issues/40)) ([a1e0c9a](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/a1e0c9abda58c8013fe74354528cd391a8b2a48d))
* another mock change ([7ca5f49](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/7ca5f49af8e07fd2f7a35abe8114aafc6722bfae))
* Avoid Keychain's errSecInteractionNotAllowed error on iOS 15 ([31a6d71](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/31a6d71f879c0e49108b05d7d52e166ce28ae5b4))
* Capacitor hook for Azure Repository ([478f73f](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/478f73f6dff0b0f83bb0ae07265bf87d88a63975))
* delete saved values on new RSA key creation ([7f6fa55](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/7f6fa5524dd59bc0983a90e8d1103b93dee68ffb))
* drop unsafe iOS keychain accessibility opts ([25c217c](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/25c217c23b93c234534e37586c84173993be8f60))
* implemented handle authentication skippd method ([31b7775](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/31b7775023c78ddf3813d8d32c67bc36837df7ac))
* improve mutual exclusion in concurrent init calls ([6b83d43](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/6b83d43361f97d9641714ec383662b7898506a81))
* migration to sharedPreferences for Android ([3f06400](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/3f064009025e780595670faecba107ca3cdcab61))
* Mock release ([6d308f2](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/6d308f2f18a444ae229985636f6bfe46a013d3e7))
* modified to test over android 6 on handle lock ([b82a686](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/b82a686d00860203c9638bbf371ea4ee696345ad))
* README example for securing device on android ([0f41f63](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/0f41f634f38994b238f101d408be01fcea435364))
* **README:** Updated README to have the name of the new library being used. ([6b40f7b](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/6b40f7b79f6a878e29c9eea64f78fe42b92ec7d1))
* remove calling of createConfirmDeviceCredentialIntent intent ([1be790e](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/1be790e7b8737d5535e500f32cb3cd3a5ea09e18))
* remove usage of createConfirmDeviceCredentialIntent altogether ([224c751](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/224c75183b3f0e5706db892ae055ea3332ae5a74))
* removed validity date limites at start and end ([c03cc01](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/c03cc019f596c197e3b6f23acd331da451dad11f))
* Replace iOS code for library ([#24](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/issues/24)) ([8a567a6](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/8a567a6891986a20291d0197c792ba0dfcdce34f))
* Return Empty Result ([#21](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/issues/21)) ([77f6d87](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/77f6d8752fa35e0da2a0e6e4f4ad6a357213caee))
* **SAMKeychain:** Changed import reference to be relative to the SAMKeychain file ([aea1eae](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/aea1eaedc1afaf186bdc9ab7b775e5dbf8075717))
* **SecureStorage:** Updated error message to inform the user to go to the correct  method. ([7d19f40](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/7d19f40c0de1a8315c25ae4554847a123c331808))
* use 28 directly instead of referencing Build.VERSION_CODES.P ([c7c3909](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/c7c3909361d0aea09848285ee5fe537938e9f68f))
* verify if the key is permanently invalidated ([ce9ad26](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/ce9ad26b1b730e9e44051240ac107b5e26d07229))


### Features

* add eslint + test migration ([f3f6ec2](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/f3f6ec2d45508cf186d63201055b2d44c12c812d))
* Create new SecureStorage implementation structure ([#16](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/issues/16)) ([8c4972e](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/8c4972e8fe12949c267c6a42b6846fe011d21c32))
* creates to unlock credentials using KeyguardManager ([b4307a5](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/b4307a57c83da303861a01de1808504fcf23e15f))
* fix migration error in migration ([9711d49](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/9711d49a1bdb339a2ffb85512301bdc64172d012))
* implement options param, cleanup code ([a095829](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/a095829202b35c5ff1e32ceb4d8a7368a7376a55))
* removes intent for Android versions > P ([20c2e5f](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/20c2e5f3cee942b1334c2b89a3a581b24cb561a9))
* throw error when value is not a string ([46a872c](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/46a872c3213919295a982e2e942769096bc1eb66))
* update android aar ([7205340](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/7205340cac3f90eb9d36f54c22ef2f4b9e2cbad0))
* use native AES on android ([492c25e](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/492c25e5a7aa7d4b27a682311b4c0ea07ab7471c))
* Use SharedPreferences for android ([911c6c4](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/911c6c4daa0b21278be9dc880db824228de50d1d))


### Reverts

* Revert "changed accessibility (i at least hope so)" ([68d896f](https://github.com/OS-pedrogustavobilro/cordova-plugin-secure-storage/commit/68d896ff60c5008ab6061ae8c2f70de21d0339d9))
