import { NativeModules, Platform } from 'react-native';

export enum CHANNEL_IMPORTANCE {
  IMPORTANCE_DEFAULT = 3,
  IMPORTANCE_HIGH = 4,
  IMPORTANCE_LOW = 2,
  IMPORTANCE_MIN = 1,
}

const DEFAULT_CHANNEL_VISIBILITY = {
  VISIBILITY_SECRET: -1,
  VISIBILITY_PRIVATE: 0,
  VISIBILITY_PUBLIC: 1,
} as const;

export const CHANNEL_VISIBILITY =
  (NativeModules.NotificationChannels?.CHANNEL_VISIBILITY as
    | typeof DEFAULT_CHANNEL_VISIBILITY
    | undefined) ?? DEFAULT_CHANNEL_VISIBILITY;

export type ChannelVisibilityValue =
  (typeof CHANNEL_VISIBILITY)[keyof typeof CHANNEL_VISIBILITY];

export type CreateChannelOptions = {
  channelId: string;
  channelName: string;
  channelDescription?: string;
  importance?: CHANNEL_IMPORTANCE;
  groupId?: string;
  lockscreenVisibility?: ChannelVisibilityValue;
  bypassDnd?: boolean;
  vibrationPattern?: number[];
};

type NotificationChannelsType = {
  listChannels(): Promise<string[] | undefined>;
  channelBlocked(channel_id: string): Promise<boolean | undefined>;
  channelExists(channel_id: string): Promise<boolean | undefined>;
  deleteChannel(channel_id: string): Promise<boolean | undefined>;
  createChannel(channelInfo: CreateChannelOptions): Promise<boolean | undefined>;
  createChannelGroup(
    groupId: string,
    groupName: string
  ): Promise<boolean | undefined>;
};

const { NotificationChannels } = NativeModules;

let NotifChannels = NotificationChannels;

if (Platform.OS === 'ios' || !NotificationChannels) {
  const iOSNotifChannels: NotificationChannelsType = {
    listChannels: async () => Promise.resolve(undefined),
    channelBlocked: async () => Promise.resolve(undefined),
    channelExists: async () => Promise.resolve(undefined),
    deleteChannel: async () => Promise.resolve(undefined),
    createChannel: async () => Promise.resolve(undefined),
    createChannelGroup: async () => Promise.resolve(undefined),
  };
  NotifChannels = iOSNotifChannels;
}

export default NotifChannels as NotificationChannelsType;
