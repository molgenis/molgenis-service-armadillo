export type Project = {
  name: string;
  users: string[];
};

export type User = {
  email: string;
  firstName: string;
  lastName: string;
  institution: string;
  admin: boolean;
  projects: string[];
};

export type Principal = {
  authorities: [
    {
      authority: string;
    }
  ];
  details: Object | null;
  authenticated: boolean;
  name: string;
  credentials: Object | null;
  principal: Object | null;
};
