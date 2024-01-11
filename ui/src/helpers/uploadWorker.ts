declare function postMessage(message: string): void;

class UploadWorker {
  onmessage(e: MessageEvent) {
    const file: File = e.data;
    const xhr = new XMLHttpRequest();
    const formData = new FormData();

    formData.append("file", file);

    xhr.open("POST", "https://your-upload-url.com", true);
    xhr.onload = function () {
      if (xhr.status === 200) {
        postMessage("Upload complete");
      } else {
        postMessage("Upload failed");
      }
    };
    xhr.send(formData);
  }
}

export { postMessage, UploadWorker };
