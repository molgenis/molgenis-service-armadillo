import {
createDownloadUrlForUserWorkspace,
createFileNameForUserWorkspace
} from "@/api/api";

describe("api", () => {
  describe("createDownloadUrlForUserWorkspace", () => {
    it("should create download url for user and workspace", () => {
      const actual = createDownloadUrlForUserWorkspace("user-j.doe__at__email.com", "my-current-research");
      expect(actual).toBe("/workspaces/download/j.doe__at__email.com/my-current-research");
    });
  });

   describe("createFileNameForUserWorkspace", () => {
    it("should create filename for user and workspace", () => {
      const actual = createFileNameForUserWorkspace("user-j.doe__at__email.com", "my-current-research");
      expect(actual).toBe("user-j.doe__at__email.com-my-current-research.RData");
    });
  });
});