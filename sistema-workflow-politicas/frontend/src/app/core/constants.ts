export const TOKEN_KEY = 'auth_token';
export const USER_KEY = 'auth_user';
export const SETTINGS_KEY = 'app_settings';

export type AppTheme = 'light' | 'dark';

export interface AppSettings {
  theme: AppTheme;
  appNotifications: boolean;
  emailNotifications: boolean;
}

export const DEFAULT_SETTINGS: AppSettings = {
  theme: 'light',
  appNotifications: true,
  emailNotifications: false,
};
