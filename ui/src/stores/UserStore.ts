import { defineStore } from "pinia";
import { deleteUser, getUsers, putUser, getProjects } from "@/api/api";

export const userStore = defineStore("users", {
  state: () => {
    return { users: [], errorMessage: "" };
  },
  actions: {
    async fetchUsers() {
      await getUsers().catch((error: string) => {
        //TODO: redirect when login error using processErrorMessages, find out how to include router using pinia
        this.errorMessage = error;
      });
    },
  },
});
