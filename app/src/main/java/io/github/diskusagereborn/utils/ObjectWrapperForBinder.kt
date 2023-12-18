package io.github.diskusagereborn.utils

import android.os.Binder
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot

class ObjectWrapperForBinder(val data : FileSystemSuperRoot) : Binder()
