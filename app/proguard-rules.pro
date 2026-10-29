# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class pk.fuelgo.driver.**$$serializer { *; }
-keepclassmembers class pk.fuelgo.driver.** { *** Companion; }
-keepclasseswithmembers class pk.fuelgo.driver.** { kotlinx.serialization.KSerializer serializer(...); }
