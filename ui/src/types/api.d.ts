import { StringArray } from "@/types/types";

export type Project = {
  name: string;
  users: string[];
};

export type RemoteFileInfo = {
  id: string;
  name: string;
};

export type RemoteFileDetail = {
  id: string;
  name: string;
  fetched: string;
  content: string;
};

export type User = {
  email: string;
  firstName: string;
  lastName: string;
  institution: string;
  admin: boolean;
  projects: string[];
};

export type UserStringKey = "email" | "firstName" | "lastName" | "institution";

export type Principal = {
  authorities: [
    {
      authority: string;
    },
  ];
  details: Object | null;
  authenticated: boolean;
  name: string;
  credentials: Object | null;
  principal: {
    authorities: Array<Object>;
    attributes: {
      applicationId: string;
      email: string;
      email_verified: boolean;
      family_name: string;
      given_name: string;
      roles: StringArray;
      sub: string;
    };
    name: string;
  } | null;
};

export type Profile = {
  name: string;
  image: string;
  host: string;
  port: number;
  packageWhitelist: StringArray;
  functionBlacklist: StringArray;
  datashieldSeed: string;
  options: {
    "datashield.seed"?: string;
  };
  container: {
    tags: StringArray;
    status: string;
  };
};

export type Auth = { user: string; pwd: string };

/**
 * Types for /actuator response
 *
 * Seems HAL API
 */
interface ActuatorLink {
  href: string;
  templated?: boolean;
}

interface HalLinks {
  [key: string]: ActuatorLink;
}

export interface HalResponse {
  _links: HalLinks;
}

/**
 * Types for /actuator/metric response.
 */
type Measurement = {
  statistic: string;
  value: number;
};

type AvailableTag = {
  tag: string;
  values: string[];
};

export type Metric = {
  name: string;
  description: string;
  baseUnit: string;
  measurements: Measurement[];
  availableTags: AvailableTag[];
  searchWords?: string;
  _display?: boolean;
};

export type Metrics = Dictionary<Metric>;
