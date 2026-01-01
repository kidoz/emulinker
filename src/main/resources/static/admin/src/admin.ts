import type { Controller, Game, ServerInfo, User, ViewName } from './types';
import './style.css';

const VIEWS: readonly ViewName[] = ['overview', 'users', 'games', 'controllers'] as const;

const VIEW_TITLES: Record<ViewName, string> = {
  overview: 'Dashboard Overview',
  users: 'Active Users',
  games: 'Game Rooms',
  controllers: 'Kaillera Controllers',
};

async function fetchData<T>(endpoint: string): Promise<T | null> {
  try {
    const response = await fetch(`/api/admin/${endpoint}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return (await response.json()) as T;
  } catch (error) {
    console.error(`Could not fetch data for ${endpoint}:`, error);
    return null;
  }
}

function getElement<T extends HTMLElement>(id: string): T {
  const element = document.getElementById(id);
  if (!element) {
    throw new Error(`Element with id "${id}" not found`);
  }
  return element as T;
}

function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function showView(viewName: ViewName): void {
  VIEWS.forEach((view) => {
    const viewElement = document.getElementById(`${view}-view`);
    if (viewElement) {
      viewElement.style.display = view === viewName ? 'block' : 'none';
    }

    const navLink = document.querySelector<HTMLElement>(`.nav-link[data-view="${view}"]`);
    if (navLink) {
      navLink.classList.toggle('active', view === viewName);
    }
  });

  getElement('view-title').textContent = VIEW_TITLES[viewName];
  void refreshData();
}

async function updateServerInfo(): Promise<void> {
  const serverInfo = await fetchData<ServerInfo>('server-info');
  if (!serverInfo) return;

  getElement('user-count').textContent = `${serverInfo.userCount} / ${serverInfo.maxUsers}`;
  getElement('game-count').textContent = `${serverInfo.gameCount} / ${serverInfo.maxGames}`;
  getElement('uptime').textContent = `${serverInfo.uptimeMinutes}m`;
  getElement('stat-requests').textContent = String(serverInfo.stats.requestCount);
  getElement('stat-connects').textContent = String(serverInfo.stats.connectCount);
  getElement('stat-errors').textContent = String(serverInfo.stats.protocolErrors);
  getElement('stat-pool').textContent =
    `${serverInfo.threadPool.active} / ${serverInfo.threadPool.poolSize}`;
}

function renderUserRow(user: User): string {
  return `
    <tr>
      <td>${user.id}</td>
      <td><span class="user-name">${escapeHtml(user.name)}</span></td>
      <td><span class="status-badge status-${user.status.toLowerCase()}">${escapeHtml(user.status)}</span></td>
      <td>${escapeHtml(user.connectionType)}</td>
      <td>${user.ping}ms</td>
      <td>${escapeHtml(user.address)}</td>
      <td>
        <button class="btn btn--danger btn--sm" data-kick-user="${user.id}">Kick</button>
      </td>
    </tr>
  `;
}

async function updateUsers(): Promise<void> {
  const users = await fetchData<User[]>('users');
  if (!users) return;

  const tbody = getElement<HTMLTableSectionElement>('users-table-body');
  tbody.innerHTML = users.map(renderUserRow).join('');
}

function renderGameRow(game: Game): string {
  return `
    <tr>
      <td>${game.id}</td>
      <td>${escapeHtml(game.rom)}</td>
      <td>${escapeHtml(game.owner)}</td>
      <td><span class="status-badge status-${game.status.toLowerCase()}">${escapeHtml(game.status)}</span></td>
      <td>${game.players}</td>
      <td>-</td>
    </tr>
  `;
}

async function updateGames(): Promise<void> {
  const games = await fetchData<Game[]>('games');
  if (!games) return;

  const tbody = getElement<HTMLTableSectionElement>('games-table-body');
  tbody.innerHTML = games.map(renderGameRow).join('');
}

function renderControllerRow(controller: Controller): string {
  return `
    <tr>
      <td>${escapeHtml(controller.version)}</td>
      <td>${controller.bufferSize}</td>
      <td>${controller.numClients}</td>
      <td>${controller.clientTypes.map(escapeHtml).join(', ')}</td>
    </tr>
  `;
}

async function updateControllers(): Promise<void> {
  const controllers = await fetchData<Controller[]>('controllers');
  if (!controllers) return;

  const tbody = getElement<HTMLTableSectionElement>('controllers-table-body');
  tbody.innerHTML = controllers.map(renderControllerRow).join('');
}

async function refreshData(): Promise<void> {
  await Promise.all([updateServerInfo(), updateUsers(), updateGames(), updateControllers()]);
}

function handleKickUser(userId: number): void {
  if (confirm(`Are you sure you want to kick user ${userId}?`)) {
    alert('Kick functionality integration pending.');
  }
}

function initializeEventListeners(): void {
  // Navigation links
  document.querySelectorAll<HTMLElement>('.nav-link[data-view]').forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const view = link.dataset.view as ViewName;
      if (view) {
        showView(view);
      }
    });
  });

  // Refresh button
  const refreshBtn = document.getElementById('refresh-btn');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => void refreshData());
  }

  // Kick user buttons (delegated)
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    const kickUserId = target.dataset.kickUser;
    if (kickUserId) {
      handleKickUser(Number(kickUserId));
    }
  });
}

function initialize(): void {
  initializeEventListeners();
  void refreshData();
  setInterval(() => void refreshData(), 10000);
}

// Start when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initialize);
} else {
  initialize();
}

// Export for potential module usage
export { showView, refreshData };
