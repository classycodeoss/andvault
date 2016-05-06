# andvault - Securely store app secrets on Android
andvault allows you to store app secrets such as passwords, encryption keys and other sensitive data in a secure way. It is similar to [SSKeyChain](https://github.com/soffes/sskeychain) on iOS.

## What does it do?
andvault provides a lean wrapper for the [Android KeyStore](http://developer.android.com/training/articles/keystore.html) mechanism introduced in Android 4.3 (API level 18). The Android KeyStore allows to securely storie a public/private keypair for an asymmetric encryption scheme (RSA), but does not provide an easy way to store a simple credential such as a password. This is in contrast to the iOS keychain, which allows storing arbitrary data.

## How does it work?
andvault created a vault key that is used to encrypt and decrypt your secrets with a symmetric algorithm (AES) at run-time. The symmetric key is "wrapped" (encrypted) with a private/public key in the secure KeyStore (the master key). The encrypted symmetric key (vault key) and the encrypted secrets can then be stored in whatever storage is appropriate for your app. The default implementation uses SharedPreferences.

The idea behind the key wrapping mechanism is described [here](http://en.wikipedia.org/wiki/Key_Wrap"), and there is some [sample code](https://android.googlesource.com/platform/development/+/master/samples/Vault/src/com/example/android/vault?autodive=0%2F) in the Android Open Source Project using it in conjunction with the KeyStore.

## Try it out
Give andvault a spin by trying out the demo app.

![Demo app screenshot 1](https://github.com/classycodeoss/andvault/raw/master/docs/demoapp-screenshot-1.png "Demo app screenshot 1")

## License
Apache 2.0
