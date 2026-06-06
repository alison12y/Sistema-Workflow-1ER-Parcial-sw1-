import { NAV_ITEMS, getVisibleNavItems } from './nav.config';
import { AuthService } from '../../services/auth.service';

describe('nav.config smart-agent visibility', () => {
  const smartAgentItem = NAV_ITEMS.find((item) => item.path === '/smart-agent');

  it('smart-agent menu item requires only AI_AGENT_USE', () => {
    expect(smartAgentItem).toBeDefined();
    expect(smartAgentItem?.permissions).toEqual(['AI_AGENT_USE']);
  });

  it('shows smart-agent only when user has AI_AGENT_USE', () => {
    const auth = {
      hasAnyPermission: (permissions: string[]) => permissions.includes('AI_AGENT_USE'),
    } as AuthService;

    const visible = getVisibleNavItems(auth).map((item) => item.path);
    expect(visible).toContain('/smart-agent');
  });

  it('hides smart-agent when user lacks AI_AGENT_USE even with TASKS_EXECUTE', () => {
    const auth = {
      hasAnyPermission: (permissions: string[]) => permissions.includes('TASKS_EXECUTE'),
    } as AuthService;

    const visible = getVisibleNavItems(auth).map((item) => item.path);
    expect(visible).not.toContain('/smart-agent');
  });
});

describe('nav.config intelligent-analytics visibility', () => {
  const analyticsItem = NAV_ITEMS.find((item) => item.path === '/intelligent-analytics');

  it('intelligent-analytics menu item requires INTELLIGENT_ANALYTICS_VIEW', () => {
    expect(analyticsItem).toBeDefined();
    expect(analyticsItem?.permissions).toEqual(['INTELLIGENT_ANALYTICS_VIEW']);
  });

  it('shows intelligent-analytics only when user has INTELLIGENT_ANALYTICS_VIEW', () => {
    const auth = {
      hasAnyPermission: (permissions: string[]) => permissions.includes('INTELLIGENT_ANALYTICS_VIEW'),
    } as AuthService;

    const visible = getVisibleNavItems(auth).map((item) => item.path);
    expect(visible).toContain('/intelligent-analytics');
  });

  it('hides intelligent-analytics when user lacks INTELLIGENT_ANALYTICS_VIEW', () => {
    const auth = {
      hasAnyPermission: (permissions: string[]) => permissions.includes('KPI_VIEW'),
    } as AuthService;

    const visible = getVisibleNavItems(auth).map((item) => item.path);
    expect(visible).not.toContain('/intelligent-analytics');
  });
});
