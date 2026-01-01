export interface ServerStats {
  requestCount: number;
  connectCount: number;
  protocolErrors: number;
  deniedFull: number;
  deniedOther: number;
}

export interface ThreadPool {
  active: number;
  poolSize: number;
  maxPoolSize: number;
  taskCount: number;
}

export interface ServerInfo {
  serverName: string;
  version: string;
  build: number;
  running: boolean;
  connectPort: number;
  uptimeMinutes: number;
  userCount: number;
  maxUsers: number;
  gameCount: number;
  maxGames: number;
  stats: ServerStats;
  threadPool: ThreadPool;
}

export interface User {
  id: number;
  name: string;
  status: string;
  connectionType: string;
  ping: number;
  address: string;
  connectTime: number;
}

export interface Game {
  id: number;
  rom: string;
  owner: string;
  status: string;
  players: number;
}

export interface Controller {
  version: string;
  bufferSize: number;
  numClients: number;
  clientTypes: string[];
}

export type ViewName = 'overview' | 'users' | 'games' | 'controllers';
