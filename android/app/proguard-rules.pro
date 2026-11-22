# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.surfswirl.bakery.** { *** Companion; }
-keepclasseswithmembers class com.surfswirl.bakery.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.surfswirl.bakery.**$$serializer { *; }
