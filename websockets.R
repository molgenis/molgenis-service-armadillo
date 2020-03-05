library(websocket)

ws <- WebSocket$new("ws://localhost:8080/ws/echo", 
                    autoConnect = FALSE,
                    headers = c(Authorization = 'Basic YWRtaW46YWRtaW4='))

ws$onOpen(function(event) {
  cat("Connection opened\n")
})
ws$onMessage(function(event) {
  cat("Client got msg: ", event$data, "\n")
})
ws$onClose(function(event) {
  cat("Client disconnected with code ", event$code,
      " and reason ", event$reason, "\n", sep = "")
})
ws$onError(function(event) {
  cat("Client failed to connect: ", event$message, "\n")
})
ws$connect()

ws$send('hi')
