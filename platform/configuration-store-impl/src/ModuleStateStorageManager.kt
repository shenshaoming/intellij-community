/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.configurationStore

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.openapi.components.StateStorage
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.TrackingPathMacroSubstitutor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.ModuleEx
import com.intellij.openapi.module.impl.ModuleManagerImpl
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.PathUtilRt
import com.intellij.util.containers.ContainerUtil

class ModuleStateStorageManager(macroSubstitutor: TrackingPathMacroSubstitutor, module: Module) : StateStorageManagerImpl("module", macroSubstitutor, module) {
  override fun createStorageData(fileSpec: String) = ModuleFileData()

  override fun startExternalization() = MyStateStorageManagerExternalizationSession(this)

  private class MyStateStorageManagerExternalizationSession(storageManager: StateStorageManagerImpl) : StateStorageManagerImpl.StateStorageManagerExternalizationSession(storageManager) {
    override fun createSaveSessions(): List<StateStorage.SaveSession> {
      val storage = ContainerUtil.getFirstItem(storageManager.getCachedFileStorages(listOf(StoragePathMacros.MODULE_FILE)))
      if (storage != null && (storage.getStorageData() as StorageData).isDirty()) {
        // force XmlElementStorageSaveSession creation
        getExternalizationSession(storage)
      }
      return super.createSaveSessions()
    }
  }

  override fun getOldStorageSpec(component: Any, componentName: String, operation: StateStorageOperation) = StoragePathMacros.MODULE_FILE

  override fun pathRenamed(oldPath: String, newPath: String, event: VFileEvent?) {
    try {
      super.pathRenamed(oldPath, newPath, event)
    }
    finally {
      val requestor = event?.getRequestor()
      if (requestor == null || requestor !is StateStorage /* not renamed as result of explicit rename */) {
        val module = componentManager as ModuleEx
        val oldName = module.getName()
        module.rename(StringUtil.trimEnd(PathUtilRt.getFileName(newPath), ModuleFileType.DOT_DEFAULT_EXTENSION))
        ModuleManagerImpl.getInstanceImpl(module.getProject()).fireModuleRenamedByVfsEvent(module, oldName)
      }
    }
  }
}