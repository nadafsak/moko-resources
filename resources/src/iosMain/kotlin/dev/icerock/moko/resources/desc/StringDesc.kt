/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.resources.desc

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.objc.pluralizedString
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat
import platform.objc.object_getClass

actual sealed class StringDesc {
    protected fun processArgs(args: List<Any>): Array<out Any> {
        return args.toList().map { (it as? StringDesc)?.localized() ?: it }.toTypedArray()
    }

    protected fun localizedString(stringRes: StringResource): String {
        var string = localeType.bundle.localizedStringForKey(stringRes.resourceId, null, null)
        if (string == stringRes.resourceId) {
            string = mainBundle.localizedStringForKey(stringRes.resourceId, null, null)
        }
        return string
    }

    actual data class Resource actual constructor(val stringRes: StringResource) : StringDesc() {
        override fun localized(): String {
            return localizedString(stringRes)
        }
    }

    actual data class ResourceFormatted actual constructor(
        val stringRes: StringResource,
        val args: List<Any>
    ) : StringDesc() {
        actual constructor(stringRes: StringResource, vararg args: Any) : this(
            stringRes,
            args.toList()
        )

        override fun localized(): String {
            return stringWithFormat(localizedString(stringRes), processArgs(args))
        }
    }

    actual data class Plural actual constructor(val pluralsRes: PluralsResource, val number: Int) :
        StringDesc() {

        override fun localized(): String {
            return pluralizedString(
                bundle = localeType.bundle,
                baseBundle = mainBundle,
                resourceId = pluralsRes.resourceId,
                number = number
            )!!
        }
    }

    actual data class PluralFormatted actual constructor(
        val pluralsRes: PluralsResource,
        val number: Int,
        val args: List<Any>
    ) : StringDesc() {

        actual constructor(pluralsRes: PluralsResource, number: Int, vararg args: Any) : this(
            pluralsRes,
            number,
            args.toList()
        )

        override fun localized(): String {
            val pluralized = pluralizedString(
                bundle = localeType.bundle,
                baseBundle = mainBundle,
                resourceId = pluralsRes.resourceId,
                number = number
            )!!
            return stringWithFormat(pluralized, processArgs(args))
        }
    }

    actual data class Raw actual constructor(val string: String) : StringDesc() {
        override fun localized(): String {
            return string
        }
    }

    actual data class Composition actual constructor(
        val args: List<StringDesc>,
        val separator: String?
    ) : StringDesc() {
        override fun localized(): String {
            return args.joinToString(separator = separator ?: "") { it.localized() }
        }
    }

    abstract fun localized(): String

    protected fun stringWithFormat(format: String, args: Array<out Any>): String {
        // NSString format works with NSObjects via %@, we should change standard format to %@
        val objcFormat = format.replace(Regex("%((?:\\.|\\d|\\$)*)[abcdefs]"), "%$1@")
        // bad but objc interop limited :(
        // When calling variadic C functions spread operator is supported only for *arrayOf(...)
        return when (args.size) {
            0 -> NSString.stringWithFormat(objcFormat)
            1 -> NSString.stringWithFormat(objcFormat, args[0])
            2 -> NSString.stringWithFormat(objcFormat, args[0], args[1])
            3 -> NSString.stringWithFormat(objcFormat, args[0], args[1], args[2])
            4 -> NSString.stringWithFormat(objcFormat, args[0], args[1], args[2], args[3])
            5 -> NSString.stringWithFormat(objcFormat, args[0], args[1], args[2], args[3], args[4])
            6 -> NSString.stringWithFormat(
                objcFormat,
                args[0],
                args[1],
                args[2],
                args[3],
                args[4],
                args[5]
            )
            7 -> NSString.stringWithFormat(
                objcFormat,
                args[0],
                args[1],
                args[2],
                args[3],
                args[4],
                args[5],
                args[6]
            )
            8 -> NSString.stringWithFormat(
                objcFormat,
                args[0],
                args[1],
                args[2],
                args[3],
                args[4],
                args[5],
                args[6],
                args[7]
            )
            9 -> NSString.stringWithFormat(
                objcFormat,
                args[0],
                args[1],
                args[2],
                args[3],
                args[4],
                args[5],
                args[6],
                args[7],
                args[8]
            )
            else -> throw IllegalArgumentException("can't handle more then 9 arguments now")
        }
    }

    actual sealed class LocaleType {
        actual class System actual constructor() : LocaleType() {
            override val bundle: NSBundle = NSBundle.bundleForClass(object_getClass(this)!!)
        }

        actual class Custom actual constructor(locale: String) : LocaleType() {
            override val bundle: NSBundle by lazy {
                val currentBundle = NSBundle.bundleForClass(object_getClass(this)!!)
                val bundlePath = currentBundle.pathForResource(locale, "lproj")
                    ?: return@lazy currentBundle
                NSBundle(bundlePath)
            }
        }

        abstract val bundle: NSBundle
    }

    @ThreadLocal
    actual companion object {
        actual var localeType: LocaleType = LocaleType.System()
        val mainBundle = LocaleType.System().bundle
    }
}
