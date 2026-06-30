const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const { spawn } = require("child_process");
const path = require("path");

const PORT = process.env.PORT || 3000;

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: "*" },
});

let mcProcess = null;
let serverDir = process.env.SERVER_DIR || process.cwd();

app.use(express.json());

io.on("connection", (socket) => {
  console.log(`Client connected: ${socket.id}`);

  socket.on("server:start", () => {
    if (mcProcess) return;
    const jar = path.join(serverDir, "server.jar");
    mcProcess = spawn("java", ["-Xmx2G", "-Xms1G", "-jar", jar, "nogui"], {
      cwd: serverDir,
    });

    mcProcess.stdout.on("data", (data) => {
      const msg = data.toString();
      socket.emit("console:log", { id: Date.now().toString(), timestamp: Date.now(), message: msg });
    });

    mcProcess.stderr.on("data", (data) => {
      const msg = data.toString();
      socket.emit("console:log", { id: Date.now().toString(), timestamp: Date.now(), message: msg, level: "error" });
    });

    mcProcess.on("exit", () => {
      mcProcess = null;
      socket.emit("server:status", { online: false });
    });

    socket.emit("server:status", { online: true });
  });

  socket.on("server:stop", () => {
    if (!mcProcess) return;
    mcProcess.stdin.write("stop\n");
  });

  socket.on("server:command", (cmd) => {
    if (!mcProcess) return;
    mcProcess.stdin.write(cmd + "\n");
  });

  socket.on("disconnect", () => {
    console.log(`Client disconnected: ${socket.id}`);
  });
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`PortalHost Agent running on port ${PORT}`);
});
