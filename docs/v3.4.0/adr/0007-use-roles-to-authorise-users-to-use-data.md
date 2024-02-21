# 7. Use roles to authorise users to use certain data

Date: 2020-04-23

## Status

Accepted

## Context

We need a way to authorise users in the MOLGENIS "Armadillo" service to use data. We want this to be as straightforward as possible.

Other techniques are ACL's for fine grained permissions on resources and ID-based. 

**ACL's**
It is more time consuming to implement ACL's. With roles the you are less flexible, but we believe this is sufficient to use DataSHIELD.

**ID-based**
The ID-based approach was suggested as a first solution to make the service secure. A big disadvantage is that anyone with the link can 
share the data. That means that there has to be a lot of trust between the data manager and the researcher. In practices, this is not feasible.

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

 
