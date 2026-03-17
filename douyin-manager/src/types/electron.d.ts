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
  syncAnchors: () => Promise<{ success: boolean; error?: string }>
  syncAnchorOnly: () => Promise<{ success: boolean; error?: string }>
  getAnchors: () => Promise<{ success: boolean; data?: any[]; error?: string }>
  getNextSerialNumber: () => Promise<{ success: boolean; data?: number; error?: string }>
  addAnchor: (data: any) => Promise<{ success: boolean; error?: string }>
  updateAnchor: (id: number, data: any) => Promise<{ success: boolean; error?: string }>
  deleteAnchor: (id: number) => Promise<{ success: boolean; error?: string }>
  addWaveStats: (data: any) => Promise<{ success: boolean; error?: string }>
  addDurationStats: (data: any) => Promise<{ success: boolean; error?: string }>
  getWaveStats: (filters?: any) => Promise<{ success: boolean; data?: any[]; error?: string }>
  getDurationStats: (filters?: any) => Promise<{ success: boolean; data?: any[]; error?: string }>
  clearStatsData: (type: 'local' | 'remote' | 'both') => Promise<{ success: boolean; results?: any; error?: string }>
  checkAuthorization: () => Promise<{ authorized: boolean; error?: string }>
  // 窗口控制
  windowMinimize: () => Promise<void>
  windowMaximize: () => Promise<void>
  windowClose: () => Promise<void>
  windowIsMaximized: () => Promise<boolean>
}

declare global {
  interface Window {
    electronAPI: ElectronAPI
  }
}

export {}
