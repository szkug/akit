// visit Glide package-visibility member, com.bumptech.glide.request
package com.bumptech.glide.request

import com.bumptech.glide.RequestBuilder


val <T> RequestBuilder<T>.autoCloneEnabled: Boolean get() = isAutoCloneEnabled