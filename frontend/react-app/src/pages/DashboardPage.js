import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { documentAPI, chatAPI } from '../services/api';
import { getUser, logout } from '../utils/auth';
import './DashboardPage.css';

function DashboardPage() {
  const [documents, setDocuments] = useState([]);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(false);
  const user = getUser();
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    loadDocuments();
    const newSessionId = Date.now().toString();
    setSessionId(newSessionId);
  }, []);

  const loadDocuments = async () => {
    try {
      const response = await documentAPI.getAll();
      setDocuments(response.data);
    } catch (err) {
      console.error('문서 목록 로드 실패:', err);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      alert('PDF 파일만 업로드 가능합니다.');
      return;
    }

    setUploading(true);
    try {
      await documentAPI.upload(file);
      alert('파일이 업로드되었습니다. 처리 중입니다...');
      loadDocuments();
      e.target.value = '';
    } catch (err) {
      alert('파일 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteDocument = async (docId) => {
    if (!window.confirm('이 문서를 삭제하시겠습니까?')) return;

    try {
      await documentAPI.delete(docId);
      loadDocuments();
    } catch (err) {
      alert('문서 삭제에 실패했습니다.');
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputMessage.trim()) return;

    const userMessage = {
      type: 'USER',
      content: inputMessage,
      timestamp: new Date().toISOString(),
    };

    setMessages([...messages, userMessage]);
    setInputMessage('');
    setLoading(true);

    try {
      const response = await chatAPI.sendMessage(inputMessage, sessionId);
      const assistantMessage = {
        type: 'ASSISTANT',
        content: response.data.response,
        sources: response.data.sources,
        timestamp: response.data.timestamp,
      };
      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      const errorMessage = {
        type: 'ASSISTANT',
        content: '죄송합니다. 응답을 생성하는 중 오류가 발생했습니다.',
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>RAG Chat System</h1>
        <div className="user-info">
          <span>환영합니다, {user?.username}님</span>
          <button onClick={handleLogout}>로그아웃</button>
        </div>
      </header>

      <div className="dashboard-content">
        <aside className="sidebar">
          <div className="document-section">
            <h2>문서 관리</h2>
            <div className="upload-section">
              <label className="upload-button">
                {uploading ? '업로드 중...' : 'PDF 업로드'}
                <input
                  type="file"
                  accept=".pdf"
                  onChange={handleFileUpload}
                  disabled={uploading}
                  style={{ display: 'none' }}
                />
              </label>
            </div>

            <div className="document-list">
              <h3>업로드된 문서</h3>
              {documents.length === 0 ? (
                <p className="no-documents">업로드된 문서가 없습니다.</p>
              ) : (
                <ul>
                  {documents.map((doc) => (
                    <li key={doc.id} className="document-item">
                      <div className="document-info">
                        <span className="document-name">{doc.originalFilename}</span>
                        <span className={`document-status ${doc.status.toLowerCase()}`}>
                          {doc.status === 'COMPLETED' ? '완료' :
                           doc.status === 'PROCESSING' ? '처리중' : '실패'}
                        </span>
                      </div>
                      <button
                        onClick={() => handleDeleteDocument(doc.id)}
                        className="delete-button"
                      >
                        삭제
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </aside>

        <main className="chat-section">
          <div className="chat-header">
            <h2>채팅</h2>
          </div>

          <div className="chat-messages">
            {messages.length === 0 ? (
              <div className="welcome-message">
                <p>문서를 업로드하고 질문을 입력해주세요.</p>
              </div>
            ) : (
              messages.map((msg, idx) => (
                <div key={idx} className={`message ${msg.type.toLowerCase()}`}>
                  <div className="message-content">{msg.content}</div>
                  {msg.sources && msg.sources.length > 0 && (
                    <div className="message-sources">
                      <strong>출처:</strong> {msg.sources.join(', ')}
                    </div>
                  )}
                </div>
              ))
            )}
            {loading && (
              <div className="message assistant">
                <div className="message-content">응답 생성 중...</div>
              </div>
            )}
          </div>

          <form onSubmit={handleSendMessage} className="chat-input-form">
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="질문을 입력하세요..."
              disabled={loading}
            />
            <button type="submit" disabled={loading || !inputMessage.trim()}>
              전송
            </button>
          </form>
        </main>
      </div>
    </div>
  );
}

export default DashboardPage;
