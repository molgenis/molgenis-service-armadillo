# Application structure

We use the **Layered Architecture** pattern, which is a common architectural pattern in object-oriented programming and particularly in Java-based web applications.

## controller

This is typically where the application's logic is defined. It handles user input and works with the model and view to perform operations and generate output.

## service

This layer is responsible for the business logic and rules of the application. It acts as a bridge between the controller and repository layers.

## model

This layer represents the data and the rules that govern access to and updates of this data. In many cases, this layer is implemented with a database.

## storage

This layer is likely responsible for data persistence.

## security

This layer is likely responsible for authentication and authorisation.

## exceptions

This is where custom exceptions for the application are defined.

## Helpers

The other directories are `audit`, `command`, `info`, `interceptor`, `metadata`, `container`: These directories could contain various utilities, helpers, or additional functionalities specific to your application.

### Container

A Docker container based on a DataShield image.
