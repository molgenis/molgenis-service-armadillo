export type Project = {
    name: string,
    users: string[]
}

export type User = {
    email: string,
    firstName: string,
    lastName: string,
    institution: string,
    admin: boolean,
    projects: string[]
}