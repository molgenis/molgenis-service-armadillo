import {
createDownloadUrlForUserWorkspace,
createFileNameForUserWorkspace,
getLatestReleaseInfo
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

  describe("getLatestReleaseInfo", () => {
    const mockRelease = (release: Object) => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => release,
      }) as any;
    };

    it("should strip the v prefix github puts on the release tag", async () => {
      mockRelease({ tag_name: "v5.1.0", html_url: "https://example.org" });
      const actual = await getLatestReleaseInfo();
      expect(actual.tag_name).toBe("5.1.0");
    });

    it("should keep the rest of the release untouched", async () => {
      mockRelease({ tag_name: "v5.1.0", html_url: "https://example.org" });
      const actual = await getLatestReleaseInfo();
      expect(actual.html_url).toBe("https://example.org");
    });

    it("should leave an already unprefixed tag unchanged", async () => {
      mockRelease({ tag_name: "5.1.0" });
      const actual = await getLatestReleaseInfo();
      expect(actual.tag_name).toBe("5.1.0");
    });
  });
});