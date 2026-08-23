# Room entities/DAOs are referenced by generated code; keep field names so
# reflection-free Room codegen (already compiled) keeps matching them.
-keep class com.menacefit.habitude.data.local.entity.** { *; }

# kotlinx.serialization uses the generated *$Companion.serializer() and the
# sealed-class subtype registry at runtime for the JSON export/import
# feature; keep serializable domain models and their serializers intact.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.menacefit.habitude.domain.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.menacefit.habitude.domain.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.menacefit.habitude.domain.**$$serializer { *; }
-keepclasseswithmembers class com.menacefit.habitude.domain.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager Workers are instantiated by reflection via their class name.
-keep class com.menacefit.habitude.notifications.** extends androidx.work.Worker
-keep class com.menacefit.habitude.notifications.** extends androidx.work.CoroutineWorker { <init>(...); }

# App widget provider is referenced by name from AndroidManifest/XML.
-keep class com.menacefit.habitude.widget.** { *; }
