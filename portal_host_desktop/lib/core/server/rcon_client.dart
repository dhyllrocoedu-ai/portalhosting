import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

class RconClient {
  final String host;
  final int port;
  final String password;
  Socket? _socket;
  int _requestId = 0;
  final StreamController<String> _outputController = StreamController<String>.broadcast();
  bool _authenticated = false;
  String _buffer = '';

  Stream<String> get outputStream => _outputController.stream;
  bool get isConnected => _socket != null && _authenticated;
  bool get isAuthenticated => _authenticated;

  RconClient({
    required this.host,
    required this.port,
    required this.password,
  });

  Future<bool> connect({Duration timeout = const Duration(seconds: 10)}) async {
    try {
      _socket = await Socket.connect(host, port, timeout: timeout);
      _socket!.setOption(SocketOption.tcpNoDelay, true);
      
      _socket!.listen(
        _onData,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: false,
      );

      // Send authentication packet
      final authSuccess = await _sendAuth();
      if (!authSuccess) {
        await disconnect();
        return false;
      }
      
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> _sendAuth() async {
    if (_socket == null) return false;
    
    final packet = _createPacket(++_requestId, 3, password);
    _socket!.add(packet);
    
    // Wait for auth response
    final completer = Completer<bool>();
    late StreamSubscription<String> sub;
    
    sub = _outputController.stream.listen((line) {
      if (line.startsWith('[AUTH]')) {
        final success = line.contains('success');
        if (!completer.isCompleted) {
          completer.complete(success);
          sub.cancel();
        }
      }
    });
    
    try {
      return await completer.future.timeout(const Duration(seconds: 5));
    } catch (_) {
      return false;
    }
  }

  Future<String?> sendCommand(String command) async {
    if (!isConnected) return null;
    
    final requestId = ++_requestId;
    final packet = _createPacket(requestId, 2, command);
    _socket!.add(packet);
    
    final completer = Completer<String?>();
    late StreamSubscription<String> sub;
    
    sub = _outputController.stream.listen((line) {
      if (line.startsWith('[RESPONSE:$requestId]')) {
        final response = line.substring('[RESPONSE:$requestId]'.length).trim();
        if (!completer.isCompleted) {
          completer.complete(response);
          sub.cancel();
        }
      }
    });
    
    try {
      return await completer.future.timeout(const Duration(seconds: 30));
    } catch (_) {
      return null;
    }
  }

  List<int> _createPacket(int requestId, int type, String body) {
    final bodyBytes = utf8.encode(body);
    final length = bodyBytes.length + 10; // 4 (length) + 4 (requestId) + 4 (type) + body + 2 (null terminators)
    final packet = BytesBuilder();
    packet.add(_intToBytes(length - 4)); // length field doesn't include itself
    packet.add(_intToBytes(requestId));
    packet.add(_intToBytes(type));
    packet.add(bodyBytes);
    packet.add([0x00, 0x00]); // null terminators
    return packet.toBytes();
  }

  List<int> _intToBytes(int value) {
    return [
      value & 0xFF,
      (value >> 8) & 0xFF,
      (value >> 16) & 0xFF,
      (value >> 24) & 0xFF,
    ];
  }

  void _onData(List<int> data) {
    _buffer += utf8.decode(data, allowMalformed: true);
    
    while (_buffer.contains('\x00\x00')) {
      final endIdx = _buffer.indexOf('\x00\x00');
      final packetStr = _buffer.substring(0, endIdx);
      _buffer = _buffer.substring(endIdx + 2);
      
      if (packetStr.length < 14) continue; // minimum packet size
      
      final bytes = utf8.encode(packetStr);
      if (bytes.length < 14) continue;
      
      // length = bytes[0..3]
      // requestId = bytes[4..7]
      // type = bytes[8..11]
      // body = bytes[12..length-2]
      final requestId = _bytesToInt(bytes, 4);
      final type = _bytesToInt(bytes, 8);
      final body = utf8.decode(bytes.sublist(12), allowMalformed: true);
      
      if (type == 2) { // SERVERDATA_RESPONSE_VALUE
        _outputController.add('[RESPONSE:$requestId]$body');
      } else if (type == 3) { // SERVERDATA_AUTH_RESPONSE
        _authenticated = requestId != -1;
        _outputController.add('[AUTH]${_authenticated ? 'success' : 'failed'}');
      }
    }
  }

  int _bytesToInt(List<int> bytes, int offset) {
    return bytes[offset] |
        (bytes[offset + 1] << 8) |
        (bytes[offset + 2] << 16) |
        (bytes[offset + 3] << 24);
  }

  void _onError(Object error) {
    _outputController.addError(error);
  }

  void _onDone() {
    _authenticated = false;
    _socket = null;
  }

  Future<void> disconnect() async {
    _socket?.destroy();
    _socket = null;
    _authenticated = false;
    _buffer = '';
  }

  void dispose() {
    disconnect();
    _outputController.close();
  }
}