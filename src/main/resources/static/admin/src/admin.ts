import type { Controller, Game, User, ViewName } from './types';
import { validateControllers, validateGames, validateServerInfo, validateUsers } from './types';
import './style.css';

const VIEWS: readonly ViewName[] = ['overview', 'users', 'games', 'controllers'] as const;
const FETCH_TIMEOUT_MS = 5000;
const REFRESH_INTERVAL_MS = 10000;

const VIEW_TITLES: Record<ViewName, string> = {
  overview: 'Dashboard Overview',
  users: 'Active Users',
  games: 'Game Rooms',
  controllers: 'Kaillera Controllers',
};

// Track interval for cleanup
let refreshIntervalId: number | null = null;

// AbortController for cancelling in-flight requests
let currentAbortController: AbortController | null = null;

/**
 * Fetch data with timeout and proper error handling
 */
async function fetchWithTimeout<T>(
  endpoint: string,
  validator: (data: unknown) => T | null
): Promise<T | null> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);

  try {
    const response = await fetch(`/api/admin/${endpoint}`, {
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data: unknown = await response.json();
    const validated = validator(data);

    if (validated === null) {
      console.error(`Validation failed for ${endpoint}: unexpected data shape`);
      return null;
    }

    return validated;
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.error(`Request timeout for ${endpoint}`);
    } else {
      console.error(`Could not fetch data for ${endpoint}:`, error);
    }
    return null;
  } finally {
    clearTimeout(timeoutId);
  }
}

/**
 * Safely get an element by ID with type checking
 */
function getElement<T extends HTMLElement>(id: string): T | null {
  return document.getElementById(id) as T | null;
}

/**
 * Create an element with optional class and text content
 */
function createElement<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  options?: { className?: string; textContent?: string }
): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  if (options?.className) {
    element.className = options.className;
  }
  if (options?.textContent !== undefined) {
    element.textContent = options.textContent;
  }
  return element;
}

/**
 * Show error banner to user
 */
function showError(message: string): void {
  const header = document.querySelector('header');
  if (!header) return;

  // Remove existing error banner if present
  const existingBanner = document.getElementById('error-banner');
  if (existingBanner) {
    existingBanner.remove();
  }

  const banner = createElement('div', {
    className: 'error-banner',
    textContent: message,
  });
  banner.id = 'error-banner';

  // Auto-hide after 5 seconds
  setTimeout(() => banner.remove(), 5000);

  header.insertAdjacentElement('afterend', banner);
}

/**
 * Clear error banner
 */
function clearError(): void {
  const banner = document.getElementById('error-banner');
  if (banner) {
    banner.remove();
  }
}

function showView(viewName: ViewName): void {
  for (const view of VIEWS) {
    const viewElement = getElement(`${view}-view`);
    if (viewElement) {
      viewElement.style.display = view === viewName ? 'block' : 'none';
    }

    const navLink = document.querySelector<HTMLElement>(`.nav-link[data-view="${view}"]`);
    if (navLink) {
      navLink.classList.toggle('active', view === viewName);
    }
  }

  const viewTitle = getElement('view-title');
  if (viewTitle) {
    viewTitle.textContent = VIEW_TITLES[viewName];
  }

  void refreshData();
}

async function updateServerInfo(): Promise<boolean> {
  const serverInfo = await fetchWithTimeout('server-info', validateServerInfo);
  if (!serverInfo) return false;

  const elements = {
    userCount: getElement('user-count'),
    gameCount: getElement('game-count'),
    uptime: getElement('uptime'),
    statRequests: getElement('stat-requests'),
    statConnects: getElement('stat-connects'),
    statErrors: getElement('stat-errors'),
    statPool: getElement('stat-pool'),
  };

  if (elements.userCount)
    elements.userCount.textContent = `${serverInfo.userCount} / ${serverInfo.maxUsers}`;
  if (elements.gameCount)
    elements.gameCount.textContent = `${serverInfo.gameCount} / ${serverInfo.maxGames}`;
  if (elements.uptime) elements.uptime.textContent = `${serverInfo.uptimeMinutes}m`;
  if (elements.statRequests)
    elements.statRequests.textContent = String(serverInfo.stats.requestCount);
  if (elements.statConnects)
    elements.statConnects.textContent = String(serverInfo.stats.connectCount);
  if (elements.statErrors)
    elements.statErrors.textContent = String(serverInfo.stats.protocolErrors);
  if (elements.statPool)
    elements.statPool.textContent = `${serverInfo.threadPool.active} / ${serverInfo.threadPool.poolSize}`;

  return true;
}

/**
 * Create a table row for a user using safe DOM methods
 */
function createUserRow(user: User): HTMLTableRowElement {
  const tr = createElement('tr');

  // ID cell
  const idCell = createElement('td', { textContent: String(user.id) });
  tr.appendChild(idCell);

  // Name cell with span
  const nameCell = createElement('td');
  const nameSpan = createElement('span', { className: 'user-name', textContent: user.name });
  nameCell.appendChild(nameSpan);
  tr.appendChild(nameCell);

  // Status cell with badge
  const statusCell = createElement('td');
  const statusBadge = createElement('span', {
    className: `status-badge status-${user.status.toLowerCase()}`,
    textContent: user.status,
  });
  statusCell.appendChild(statusBadge);
  tr.appendChild(statusCell);

  // Connection type cell
  const connCell = createElement('td', { textContent: user.connectionType });
  tr.appendChild(connCell);

  // Ping cell
  const pingCell = createElement('td', { textContent: `${user.ping}ms` });
  tr.appendChild(pingCell);

  // Address cell
  const addrCell = createElement('td', { textContent: user.address });
  tr.appendChild(addrCell);

  // Actions cell
  const actionsCell = createElement('td');
  const kickBtn = createElement('button', {
    className: 'btn btn--danger btn--sm',
    textContent: 'Kick',
  });
  kickBtn.dataset.kickUser = String(user.id);
  kickBtn.setAttribute('aria-label', `Kick user ${user.name}`);
  actionsCell.appendChild(kickBtn);
  tr.appendChild(actionsCell);

  return tr;
}

async function updateUsers(): Promise<boolean> {
  const users = await fetchWithTimeout('users', validateUsers);
  if (!users) return false;

  const tbody = getElement<HTMLTableSectionElement>('users-table-body');
  if (!tbody) return false;

  // Clear existing rows
  tbody.replaceChildren();

  // Add new rows using DOM methods
  for (const user of users) {
    tbody.appendChild(createUserRow(user));
  }

  return true;
}

/**
 * Create a table row for a game using safe DOM methods
 */
function createGameRow(game: Game): HTMLTableRowElement {
  const tr = createElement('tr');

  // ID cell
  tr.appendChild(createElement('td', { textContent: String(game.id) }));

  // ROM cell
  tr.appendChild(createElement('td', { textContent: game.rom }));

  // Owner cell
  tr.appendChild(createElement('td', { textContent: game.owner }));

  // Status cell with badge
  const statusCell = createElement('td');
  const statusBadge = createElement('span', {
    className: `status-badge status-${game.status.toLowerCase()}`,
    textContent: game.status,
  });
  statusCell.appendChild(statusBadge);
  tr.appendChild(statusCell);

  // Players cell
  tr.appendChild(createElement('td', { textContent: String(game.players) }));

  // Actions cell (placeholder)
  tr.appendChild(createElement('td', { textContent: '-' }));

  return tr;
}

async function updateGames(): Promise<boolean> {
  const games = await fetchWithTimeout('games', validateGames);
  if (!games) return false;

  const tbody = getElement<HTMLTableSectionElement>('games-table-body');
  if (!tbody) return false;

  tbody.replaceChildren();

  for (const game of games) {
    tbody.appendChild(createGameRow(game));
  }

  return true;
}

/**
 * Create a table row for a controller using safe DOM methods
 */
function createControllerRow(controller: Controller): HTMLTableRowElement {
  const tr = createElement('tr');

  tr.appendChild(createElement('td', { textContent: controller.version }));
  tr.appendChild(createElement('td', { textContent: String(controller.bufferSize) }));
  tr.appendChild(createElement('td', { textContent: String(controller.numClients) }));
  tr.appendChild(createElement('td', { textContent: controller.clientTypes.join(', ') }));

  return tr;
}

async function updateControllers(): Promise<boolean> {
  const controllers = await fetchWithTimeout('controllers', validateControllers);
  if (!controllers) return false;

  const tbody = getElement<HTMLTableSectionElement>('controllers-table-body');
  if (!tbody) return false;

  tbody.replaceChildren();

  for (const controller of controllers) {
    tbody.appendChild(createControllerRow(controller));
  }

  return true;
}

async function refreshData(): Promise<void> {
  // Cancel any in-flight requests
  if (currentAbortController) {
    currentAbortController.abort();
  }
  currentAbortController = new AbortController();

  const results = await Promise.all([
    updateServerInfo(),
    updateUsers(),
    updateGames(),
    updateControllers(),
  ]);

  const allSucceeded = results.every((r) => r);
  if (allSucceeded) {
    clearError();
  } else {
    const failedCount = results.filter((r) => !r).length;
    if (failedCount === results.length) {
      showError('Unable to connect to server. Check if the server is running.');
    } else {
      showError(`Some data failed to load (${failedCount}/${results.length} requests failed)`);
    }
  }
}

function handleKickUser(userId: number, userName: string): void {
  if (confirm(`Are you sure you want to kick user "${userName}" (ID: ${userId})?`)) {
    // TODO: Implement kick API call
    // For now, show a message that it's not yet implemented
    showError('Kick functionality is not yet implemented on the server.');
  }
}

function initializeEventListeners(): void {
  // Navigation links - use event delegation
  const navContainer = document.querySelector('.nav-links');
  if (navContainer) {
    navContainer.addEventListener('click', (e) => {
      const target = e.target as HTMLElement;
      const link = target.closest<HTMLAnchorElement>('.nav-link[data-view]');
      if (link) {
        e.preventDefault();
        const view = link.dataset.view;
        if (view && VIEWS.includes(view as ViewName)) {
          showView(view as ViewName);
        }
      }
    });
  }

  // Refresh button
  const refreshBtn = getElement('refresh-btn');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', () => void refreshData());
  }

  // Kick user buttons (delegated to document for dynamic content)
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    if (target.dataset.kickUser) {
      const userId = Number(target.dataset.kickUser);
      // Try to get the user name from the same row
      const row = target.closest('tr');
      const nameCell = row?.querySelector('.user-name');
      const userName = nameCell?.textContent ?? `User ${userId}`;

      if (!Number.isNaN(userId)) {
        handleKickUser(userId, userName);
      }
    }
  });
}

function cleanup(): void {
  // Clear refresh interval
  if (refreshIntervalId !== null) {
    clearInterval(refreshIntervalId);
    refreshIntervalId = null;
  }

  // Abort any pending requests
  if (currentAbortController) {
    currentAbortController.abort();
    currentAbortController = null;
  }
}

function initialize(): void {
  // Clean up any previous state (useful for hot module reload)
  cleanup();

  initializeEventListeners();
  void refreshData();

  // Store interval ID for potential cleanup
  refreshIntervalId = window.setInterval(() => void refreshData(), REFRESH_INTERVAL_MS);
}

// Clean up on page unload
window.addEventListener('beforeunload', cleanup);

// Start when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initialize);
} else {
  initialize();
}
