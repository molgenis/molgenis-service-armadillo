# 7. Use roles to authorise users to use certain data

Date: 2020-04-23

## Status

Accepted

## Context

We need a way to authorise users in the MOLGENIS "Armadillo" service to use data. We want this to be as straightforward as possible.

## Decision

We will use roles as a basic principle to give users permission on the data they need to have access to. 
Within the MOLGENIS "Armadillo" service there are at least these 2 types of roles:
- Researcher
  *Implicit permissions*
  - READ --> on data in the shared folder(s) to which they have access
  - ADMIN --> on data in their own (user)folder
- Data manager
  *Implicit permissions*
  - ADMIN --> on all that lives
Each researcher role will have corresponding folder. The data manager is able to do anything anywhere.

## Consequences
- In the file storage you need to be aware of the fact that you are using roles as an authorisation mechanism 
- It is not possible to have fine grained permissions within a role

 
