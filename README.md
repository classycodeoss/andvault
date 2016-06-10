# andvault - Securely store app secrets on Android
andvault allows you to store app secrets such as passwords, encryption keys and other sensitive data in a secure way. It is similar to [SSKeyChain](https://github.com/soffes/sskeychain) on iOS.

## What does it do?
andvault provides a lean wrapper for the [Android KeyStore](http://developer.android.com/training/articles/keystore.html) mechanism introduced in Android 4.3 (API level 18). The Android KeyStore allows to securely storie a public/private keypair for an asymmetric encryption scheme (RSA), but does not provide an easy way to store a simple credential such as a password. This is in contrast to the iOS keychain, which allows storing arbitrary data.

## How does it work?
andvault created a vault key that is used to encrypt and decrypt your secrets with a symmetric algorithm (AES) at run-time. The symmetric key is "wrapped" (encrypted) with a private/public key in the secure KeyStore (the master key). The encrypted symmetric key (vault key) and the encrypted secrets can then be stored in whatever storage is appropriate for your app. The default implementation uses SharedPreferences.

The idea behind the key wrapping mechanism is described [here](http://en.wikipedia.org/wiki/Key_Wrap"), and there is some [sample code](https://android.googlesource.com/platform/development/+/master/samples/Vault/src/com/example/android/vault?autodive=0%2F) in the Android Open Source Project using it in conjunction with the KeyStore.

## Hardware-backed Keystore
As part of the development of andvault, we also ran tests on Amazon Device Farm to determine how widespread hardware support for the Android Keystore Provider is. What we did is basically run a unit test with the following snippet on each device:
```java
android.security.KeyChain.isBoundKeyAlgorithm("RSA")
```

The javadoc of said class says this is an appropriate way to check if there is hardware support for the private key. To quote https://developer.android.com/reference/android/security/KeyChain.html:

> This can be used to tell if there is special hardware support that can be used to bind keys to the device in a way that makes it non-exportable.

### Devices that have hardware support for Android Keystore Provider

| Device       | Android Version |
|------------- |--------------- |
| ASUS Memo Pad 7  | 5.0 |
| ASUS Memo Pad 8  | 4.4.2 |
| ASUS Nexus 7 - 1st Gen (WiFi)  | 4.4.2 |
| ASUS Nexus 7 - 1st Gen (WiFi)  | 4.3 |
| ASUS Nexus 7 - 2nd Gen  | 6.0 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 4.4.4 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 5.0 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 5.0.2 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 5.0.1 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 4.3.1 |
| ASUS Nexus 7 - 2nd Gen (WiFi)  | 4.4.2 |
| ASUS Transformer Pad 10.1 K014  | 4.4.2 |
| Amazon Fire (2015)  | 5.1 |
| Amazon Fire Phone  | 4.4.4 |
| Dell Venue 8 7840  | 5.1 |
| HTC One M7 (AT&T)  | 4.4.2 |
| HTC One M8 (AT&T)  | 4.4.4 |
| HTC One M8 (AT&T)  | 4.4.2 |
| HTC One M8 (Sprint)  | 4.4.4 |
| HTC One M8 (Verizon)  | 4.4.3 |
| HTC One M8 (Verizon)  | 4.4.2 |
| HTC One M8 (Verizon)  | 4.4.4 |
| HTC One M9 (AT&T)  | 5.0.2 |
| HTC One M9 (Sprint)  | 5.0.2 |
| HTC One M9 (Verizon)  | 5.0.2 |
| Huawei Nexus 6P  | 6.0 |
| LG G Flex2 (Sprint)  | 5.0.1 |
| LG G Pad 7.0 (AT&T)  | 4.4.2 |
| LG G3 (AT&T)  | 5.0.1 |
| LG G3 (AT&T)  | 4.4.2 |
| LG G3 (Sprint)  | 5.0.1 |
| LG G3 (Sprint)  | 4.4.2 |
| LG G3 (Verizon)  | 4.4.2 |
| LG G4 (Verizon)  | 5.1 |
| LG Nexus 4  | 4.4.3 |
| LG Nexus 5  | 5.0.1 |
| LG Nexus 5  | 4.4 |
| LG Nexus 5  | 5.1.1 |
| LG Nexus 5  | 4.4.3 |
| LG Nexus 5  | 6.0 |
| LG Nexus 5  | 4.4.2 |
| LG Nexus 5X  | 6.0 |
| LG Optimus L70 (MetroPCS)  | 4.4.2 |
| LG Optimus L90 (T-Mobile)  | 4.4.2 |
| Motorola DROID RAZR M (Verizon)  | 4.4.2 |
| Motorola DROID Turbo (Verizon)  | 5.1 |
| Motorola DROID Ultra (Verizon)  | 4.4.4 |
| Motorola Moto E - 2nd Gen  | 5.0.2 |
| Motorola Moto G (AT&T)  | 4.4.4 |
| Motorola Moto G - 2nd Gen  | 5.0.2 |
| Motorola Moto X (Verizon)  | 5.1 |
| Motorola Nexus 6  | 5.0 |
| Motorola Nexus 6  | 5.1 |
| Motorola Nexus 6  | 6.0 |
| OnePlus One  | 4.4.4 |
| Samsung GALAXY GRAND 2  | 4.4.2 |
| Samsung Galaxy E7  | 4.4.4 |
| Samsung Galaxy Grand Prime Duos  | 4.4.4 |
| Samsung Galaxy Light (MetroPCS)  | 4.4.2 |
| Samsung Galaxy Note 3 (AT&T)  | 4.4.2 |
| Samsung Galaxy Note 3 (Sprint)  | 4.4.4 |
| Samsung Galaxy Note 3 (Verizon)  | 4.4.4 |
| Samsung Galaxy Note 4 (AT&T)  | 5.0.1 |
| Samsung Galaxy Note 4 (AT&T)  | 4.4.4 |
| Samsung Galaxy Note 4 (Sprint)  | 4.4.4 |
| Samsung Galaxy Note 4 (T-Mobile)  | 4.4.4 |
| Samsung Galaxy Note 4 (Verizon)  | 5.0.1 |
| Samsung Galaxy Note 4 (Verizon)  | 4.4.4 |
| Samsung Galaxy Note 5  | 5.1.1 |
| Samsung Galaxy Note II (AT&T)  | 4.4.2 |
| Samsung Galaxy Note II (Verizon)  | 4.4.2 |
| Samsung Galaxy S4 (AT&T)  | 4.4.4 |
| Samsung Galaxy S4 (AT&T)  | 5.0.1 |
| Samsung Galaxy S4 (Sprint)  | 4.4.2 |
| Samsung Galaxy S4 (T-Mobile)  | 4.4.4 |
| Samsung Galaxy S4 (US Cellular)  | 4.4.2 |
| Samsung Galaxy S4 (Verizon)  | 5.0.1 |
| Samsung Galaxy S4 Active (AT&T)  | 4.4.2 |
| Samsung Galaxy S4 mini (Verizon)  | 4.4.2 |
| Samsung Galaxy S5 (AT&T)  | 4.4.2 |
| Samsung Galaxy S5 (AT&T)  | 4.4.4 |
| Samsung Galaxy S5 (Sprint)  | 4.4.4 |
| Samsung Galaxy S5 (T-Mobile)  | 4.4.2 |
| Samsung Galaxy S5 (Verizon)  | 4.4.4 |
| Samsung Galaxy S5 Active (AT&T)  | 4.4.2 |
| Samsung Galaxy S6 (Verizon)  | 5.0.2 |
| Samsung Galaxy S6 Edge (Verizon)  | 5.0.2 |
| Samsung Galaxy S6 Edge+ (AT&T)  | 5.1.1 |
| Samsung Galaxy Tab 4 10.1 Nook (WiFi)  | 5.0.2 |
| Samsung Galaxy Tab 4 10.1 Nook (WiFi)  | 4.4.2 |
| Sony Xperia Z2 (GSM)  | 4.4.4 |
| Sony Xperia Z3 Compact (GSM)  | 4.4.4 |
| Toshiba Excite Go 7  | 4.4.2 |
| Venue 8 3840 Red (tablet)  | 4.4.4 |

### Devices that do *NOT* have support

| Device       | Android Version |
|------------- |--------------- |
| Amazon Kindle Fire HD 7 (WiFi)  | 4.4.3 |
| Amazon Kindle Fire HDX 7 (WiFi)  | 4.4.3 |
| Intex Intex Aqua Y2 Pro  | 4.4.2 |
| LG G2 (AT&T)  | 4.4.2 |
| LG G2 (T-Mobile)  | 4.4.2 |
| Micromax Bolt S300  | 4.4.3 |
| Samsung Galaxy Grand Neo Plus  | 4.4.4 |
| Samsung Galaxy J1 Duos  | 4.4.4 |
| Samsung Galaxy Note II (AT&T)  | 4.3 |
| Samsung Galaxy S3 (AT&T)  | 4.3 |
| Samsung Galaxy S3 (Sprint)  | 4.3 |
| Samsung Galaxy S3 (Sprint)  | 4.4.2 |
| Samsung Galaxy S3 (T-Mobile)  | 4.3 |
| Samsung Galaxy S3 (Verizon)  | 4.4.2 |
| Samsung Galaxy S3 (Verizon)  | 4.3 |
| Samsung Galaxy S3 LTE (T-Mobile)  | 4.3 |
| Samsung Galaxy S3 Mini (AT&T)  | 4.4.2 |
| Samsung Galaxy Tab 3 7.0 (WiFi)  | 4.4.2 |
| Samsung Galaxy Tab 4 7.0 Nook (WiFi)  | 4.4.2 |

## Try it out
Give andvault a spin by trying out the demo app.

![Demo app screenshot 1](https://github.com/classycodeoss/andvault/raw/master/docs/demoapp-screenshot-1.png "Demo app screenshot 1")

## License
Apache 2.0
