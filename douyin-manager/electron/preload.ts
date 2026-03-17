import { contextBridge, ipcRenderer } from 'electron'

export interface ElectronAPI {
  openFileDialog: (filters?: { name: string; extensions: string[] }[]) => Promise<string[]>
  openSaveDialog: (options?: { defaultPath?: string; filters?: { name: string; extensions: string[] }[] }) => Promise<{ canceled: boolean; filePath?: string }>
  readFile: (path: string) => Promise<{ success: boolean; data?: string; error?: string }>
  writeFile: (path: string, content: string) => Promise<{ success: boolean; error?: string }>
  copyFile: (source: string, dest: string) => Promise<{ success: boolean; error?: string }>
  getDirName: (path: string) => Promise<string>
  getBasename: (path: string) => Promise<string>
  getExtName: (path: string) => Promise<string>
  joinPath: (...paths: string[]) => Promise<string>
  syncAnchors: () => Promise<{ success: boolean; data?: any; error?: string }>
  syncAllData: () => Promise<{ success: boolean; data?: any; error?: string }>
  getAnchors: () => Promise<{ success: boolean; data?: any[]; error?: string }>
  getNextSerialNumber: () => Promise<{ success: boolean; data?: number; error?: string }>
  addAnchor: (data: any) => Promise<{ success: boolean; error?: string }>
  updateAnchor: (id: number, data: any) => Promise<{ success: boolean; error?: string }>
  deleteAnchor: (id: number) => Promise<{ success: boolean; error?: string }>
  addWaveStats: (data: any) => Promise<{ success: boolean; error?: string }>
  addDurationStats: (data: any) => Promise<{ success: boolean; error?: string }>
  getWaveStats: (filters?: any) => Promise<{ success: boolean; data?: any[]; error?: string }>
  getDurationStats: (filters?: any) => Promise<{ success: boolean; data?: any[]; error?: string }>
  clearStatsData: (type: 'local' | 'remote' | 'both') => Promise<{ success: boolean; results?: any; warnings?: string[]; error?: string }>
  checkAuthorization: () => Promise<{ authorized: boolean; error?: string }>
  // 窗口控制
  windowMinimize: () => Promise<void>
  windowMaximize: () => Promise<void>
  windowClose: () => Promise<void>
  windowIsMaximized: () => Promise<boolean>
}

const api: ElectronAPI = {
  openFileDialog: (filters) => ipcRenderer.invoke('dialog:openFile', filters),
  openSaveDialog: (options) => ipcRenderer.invoke('dialog:saveFile', options),
  readFile: (path) => ipcRenderer.invoke('fs:readFile', path),
  writeFile: (path, content) => ipcRenderer.invoke('fs:writeFile', path, content),
  copyFile: (source, dest) => ipcRenderer.invoke('fs:copyFile', source, dest),
  getDirName: (path) => ipcRenderer.invoke('path:dirname', path),
  getBasename: (path) => ipcRenderer.invoke('path:basename', path),
  getExtName: (path) => ipcRenderer.invoke('path:extname', path),
  joinPath: (...paths) => ipcRenderer.invoke('path:join', ...paths),
  syncAnchors: () => ipcRenderer.invoke('db:syncAllData'),
  syncAllData: () => ipcRenderer.invoke('db:syncAllData'),
  getAnchors: () => ipcRenderer.invoke('db:getAnchors'),
  getNextSerialNumber: () => ipcRenderer.invoke('db:getNextSerialNumber'),
  addAnchor: (data) => ipcRenderer.invoke('db:addAnchor', data),
  updateAnchor: (id, data) => ipcRenderer.invoke('db:updateAnchor', id, data),
  deleteAnchor: (id) => ipcRenderer.invoke('db:deleteAnchor', id),
  addWaveStats: (data) => ipcRenderer.invoke('db:addWaveStats', data),
  addDurationStats: (data) => ipcRenderer.invoke('db:addDurationStats', data),
  getWaveStats: (filters) => ipcRenderer.invoke('db:getWaveStats', filters),
  getDurationStats: (filters) => ipcRenderer.invoke('db:getDurationStats', filters),
  clearStatsData: (type) => ipcRenderer.invoke('db:clearStatsData', type),
  checkAuthorization: () => ipcRenderer.invoke('db:checkAuthorization'),
  // 窗口控制
  windowMinimize: () => ipcRenderer.invoke('window:minimize'),
  windowMaximize: () => ipcRenderer.invoke('window:maximize'),
  windowClose: () => ipcRenderer.invoke('window:close'),
  windowIsMaximized: () => ipcRenderer.invoke('window:isMaximized')
}

contextBridge.exposeInMainWorld('electronAPI', api)
