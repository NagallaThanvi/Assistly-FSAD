import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import RequestMap from './RequestMap';

const UserDashboard = () => {
  const [user, setUser] = useState({ id: null, name: 'User', role: 'User', token: '' });
  const [requests, setRequests] = useState([]);
  const [communities, setCommunities] = useState([]);
  
  const [activeTab, setActiveTab] = useState('dashboard'); // 'dashboard' | 'communities' | 'profile' | 'admin_manage'
  const [activeAdminComm, setActiveAdminComm] = useState(null); // The community the admin is managing
  const [selectedCommunityId, setSelectedCommunityId] = useState(null); // For viewing requests contextually

  const [isVolunteerMode, setIsVolunteerMode] = useState(false);
  const navigate = useNavigate();

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [showCreateCommModal, setShowCreateCommModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  
  const [formData, setFormData] = useState({ title: '', description: '', latitude: 40.7128, longitude: -74.0060 });
  const [commFormData, setCommFormData] = useState({ name: '', description: '', isPrivate: false });
  const [profileData, setProfileData] = useState(null);
  
  const apiUrl = 'http://localhost:8080/api';

  const normalizeId = (value) => {
    if (value === null || value === undefined || value === '') return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  };

  const sameId = (a, b) => {
    const left = normalizeId(a);
    const right = normalizeId(b);
    return left !== null && right !== null && left === right;
  };

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser);
        setUser({ 
          id: parsed.id || null,
          name: parsed.name || 'User', 
          role: parsed.role || 'User',
          token: parsed.token || ''
        });
        fetchCommunities(parsed.token);
      } catch (err) {
        console.error('Error parsing user data', err);
      }
    } else {
      navigate('/login');
    }
  }, [navigate]);

  useEffect(() => {
    // Proactive selection: If we have communities and none is selected, pick the first one joined
    if (communities.length > 0 && !selectedCommunityId) {
      const joinedComm = communities.find(c => 
        c.members?.some(m => sameId(m.id, user.id)) || 
        sameId(c.admin?.id, user.id)
      );
      if (joinedComm) {
        console.log("SYNC: Auto-transitioning to Syndicate Context:", joinedComm.name);
        setSelectedCommunityId(joinedComm.id);
      }
    }
  }, [communities, user.id, selectedCommunityId]);

  // Keep selectedCommunityId valid if communities change
  useEffect(() => {
    if (selectedCommunityId && communities.length > 0) {
        const stillExists = communities.find(c => sameId(c.id, selectedCommunityId));
        if (!stillExists) {
            setSelectedCommunityId(null);
        }
    }
  }, [communities, selectedCommunityId]);

  useEffect(() => {
    if (selectedCommunityId && user.token) {
      fetchRequests(user.token, selectedCommunityId);
    }
  }, [selectedCommunityId, user.token]);

  useEffect(() => {
    if (activeTab === 'profile' && user.token) {
      fetchProfile(user.token);
    }
  }, [activeTab, user.token]);

  const fetchProfile = async (token) => {
    try {
      const res = await axios.get(`${apiUrl}/users/profile`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setProfileData(res.data);
    } catch (err) {
      console.error("Failed to fetch profile", err);
    }
  };

  const fetchRequests = async (token, commId) => {
    if (!commId) return;
    try {
      const res = await axios.get(`${apiUrl}/requests/community/${commId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setRequests(res.data);
    } catch (err) {
      if (err.response?.status !== 403) {
         console.error("Failed to fetch requests", err);
      }
    }
  };

  const fetchCommunities = async (token) => {
    try {
      const res = await axios.get(`${apiUrl}/communities`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setCommunities(res.data);
    } catch (err) {
      console.error('Failed to fetch communities', err);
    }
  };

  const toggleMode = async () => {
    setIsVolunteerMode(!isVolunteerMode);
  };

  const handleJoinCommunity = async (id) => {
    try {
      await axios.post(`${apiUrl}/communities/${id}/join`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      fetchCommunities(user.token);
    } catch (err) {
      alert(err.response?.data || "Failed to join");
    }
  };

  const handleAdminAction = async (commId, userId, actionType) => {
    try {
       if (actionType === 'remove') {
           if (!window.confirm("Remove this member?")) return;
           await axios.delete(`${apiUrl}/communities/${commId}/members/${userId}`, {
               headers: { Authorization: `Bearer ${user.token}` }
           });
       } else {
           await axios.post(`${apiUrl}/communities/${commId}/members/${userId}/${actionType}`, {}, {
               headers: { Authorization: `Bearer ${user.token}` }
           });
       }
       fetchCommunities(user.token);
       const res = await axios.get(`${apiUrl}/communities`, { headers: { Authorization: `Bearer ${user.token}` } });
       const updatedComm = res.data.find(c => c.id === commId);
       setActiveAdminComm(updatedComm);
    } catch (err) {
       alert(err.response?.data || `Failed to ${actionType} user`);
    }
  };

  const handleVerifyAction = async (id, action) => {
    try {
      await axios.post(`${apiUrl}/requests/${id}/${action}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      fetchRequests(user.token, selectedCommunityId);
    } catch (err) {
      alert(err.response?.data || `Unable to ${action} request at this time.`);
    }
  };

  const handleCreateRequest = async (e) => {
    e.preventDefault();
    if (!selectedCommunityId) {
        alert("Select a workspace community from the dashboard context tab first!");
        return;
    }
    setLoading(true);
    try {
        await axios.post(`${apiUrl}/requests/community/${selectedCommunityId}`, formData, { headers: { Authorization: `Bearer ${user.token}` } });
        setShowModal(false);
        setFormData({ title: '', description: '', latitude: 40.7128, longitude: -74.0060 });
        fetchRequests(user.token, selectedCommunityId);
    } catch (err) {
      const message = err.response?.data || "Unable to create request right now.";
      alert(typeof message === 'string' ? message : "Unable to create request right now.");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCommunity = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
        await axios.post(`${apiUrl}/communities`, commFormData, { headers: { Authorization: `Bearer ${user.token}` } });
        setShowCreateCommModal(false);
        setCommFormData({ name: '', description: '', isPrivate: false });
        fetchCommunities(user.token);
    } catch (err) {
      alert(err.response?.data || "Unable to create community.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    navigate('/login');
  };

  const displayedRequests = requests.filter(req => {
    if (isVolunteerMode) return !sameId(req.author?.id, user.id);
    return sameId(req.author?.id, user.id);
  });

  return (
    <div className="container mt-4 pt-4 mb-5">
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
        <div>
          <h2 className="text-gradient">Welcome, {user.name}</h2>
        </div>
        <div className="d-flex gap-3 align-items-center">
          <div className="form-check form-switch ps-0 d-flex align-items-center gap-2">
            <span className={`small fw-bold ${!isVolunteerMode ? 'text-gradient' : 'text-muted'}`}>RESIDENT</span>
            <input className="form-check-input ms-0 mt-0" type="checkbox" role="switch" checked={isVolunteerMode} onChange={toggleMode} style={{width: '3em', height: '1.5em', cursor: 'pointer'}} />
            <span className={`small fw-bold ${isVolunteerMode ? 'text-info' : 'text-muted'}`}>VOLUNTEER</span>
          </div>
          <button onClick={handleLogout} className="neon-button-secondary py-1 px-3">Logout</button>
        </div>
      </div>

      <div className="row g-4">
        {/* Sidebar */}
        <div className="col-lg-3">
          <div className="glass-card h-100 d-flex flex-column align-items-center py-4">
            <div className="rounded-circle bg-primary bg-opacity-25 mb-3 d-flex align-items-center justify-content-center text-primary" style={{ width: '80px', height: '80px', fontSize: '1.8rem', border: '2px solid rgba(59, 130, 246, 0.3)' }}>
              {user.name.charAt(0).toUpperCase()}
            </div>
            <h5 className="fw-bold text-center">{user.name}</h5>
            <span className="badge bg-primary bg-opacity-25 text-primary border border-primary rounded-pill px-3 py-1 mb-4">{user.role}</span>
            <div className="d-flex flex-column gap-2 w-100 px-2 mt-auto">
              <button 
                onClick={() => { 
                  if (!selectedCommunityId) {
                    const firstJoined = communities.find(c => c.members?.some(m => sameId(m.id, user.id)) || sameId(c.admin?.id, user.id));
                    if (firstJoined) {
                      setSelectedCommunityId(firstJoined.id);
                      setShowModal(true);
                    } else {
                      setActiveTab('communities');
                      alert("Intelligence Protocol: Join a Syndicate Network (Community) first to broadcast requests.");
                    }
                  } else {
                    setShowModal(true); 
                  }
                }} 
                className="btn btn-outline-light rounded-pill w-100"
              >
                <i className="bi bi-plus-circle me-2"></i> New Request
              </button>
              <button onClick={() => setActiveTab('dashboard')} className={`btn rounded-pill w-100 mt-3 ${activeTab === 'dashboard' ? 'btn-primary shadow-sm' : 'btn-outline-secondary border-0'}`}>
                <i className="bi bi-grid-1x2-fill me-2"></i> Workspace
              </button>
              <button onClick={() => setActiveTab('communities')} className={`btn rounded-pill w-100 ${activeTab === 'communities' ? 'btn-primary shadow-sm' : 'btn-outline-secondary border-0'}`}>
                <i className="bi bi-people-fill me-2"></i> Syndicate Network
              </button>
              <button onClick={() => setActiveTab('profile')} className={`btn rounded-pill w-100 ${activeTab === 'profile' ? 'btn-primary shadow-sm' : 'btn-outline-secondary border-0'}`}>
                <i className="bi bi-person-circle me-2"></i> Profile Hub
              </button>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="col-lg-9">
          {activeTab === 'dashboard' ? (
            <div className="glass-card h-100">
              <div className="d-flex justify-content-between mb-4 align-items-center">
                  <h4 className="mb-0">{isVolunteerMode ? 'Global Assignments' : 'My Requests'}</h4>
                  <div className="d-flex gap-3 align-items-center w-50 justify-content-end">
                    <select className="form-select bg-dark text-light border-secondary" value={selectedCommunityId || ''} onChange={(e) => setSelectedCommunityId(Number(e.target.value))}>
                        <option value="" disabled>Select Workspace...</option>
                          {communities.filter(c => c.members?.some(m => sameId(m.id, user.id)) || sameId(c.admin?.id, user.id)).map(c => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                    </select>
                  </div>
              </div>

              {selectedCommunityId && (
                <div className="row g-3 mb-4">
                  <div className="col-md-4">
                    <div className="p-3 bg-dark bg-opacity-25 border border-secondary rounded-4 text-center stat-card">
                      <div className="small text-muted mb-1">Active Missions</div>
                      <div className="h3 fw-bold text-info mb-0">{requests.filter(r => r.status === 'IN_PROGRESS' || r.status === 'OPEN').length}</div>
                    </div>
                  </div>
                  <div className="col-md-4">
                    <div className="p-3 bg-dark bg-opacity-25 border border-secondary rounded-4 text-center stat-card">
                      <div className="small text-muted mb-1">Pending Verification</div>
                      <div className="h3 fw-bold text-warning mb-0">{requests.filter(r => r.status === 'PENDING_VERIFICATION').length}</div>
                    </div>
                  </div>
                  <div className="col-md-4">
                    <div className="p-3 bg-dark bg-opacity-25 border border-secondary rounded-4 text-center stat-card">
                      <div className="small text-muted mb-1">Missions Completed</div>
                      <div className="h3 fw-bold text-success mb-0">{requests.filter(r => r.status === 'COMPLETED').length}</div>
                    </div>
                  </div>
                </div>
              )}

              {selectedCommunityId ? (
                <>
                  <div className="table-responsive mb-4" style={{maxHeight: '300px'}}>
                    <table className="table table-borderless text-light align-middle custom-glass-table">
                      <thead className="position-sticky top-0 bg-dark" style={{zIndex: 1}}>
                        <tr className="text-muted border-bottom border-light border-opacity-10">
                          <th scope="col" className="pb-3 fw-medium">Mission Title</th>
                          <th scope="col" className="pb-3 fw-medium text-center">State Status</th>
                          <th scope="col" className="pb-3 fw-medium text-end">Action Interface</th>
                        </tr>
                      </thead>
                      <tbody>
                        {displayedRequests.length > 0 ? displayedRequests.map(req => (
                          <tr key={req.id} className="border-bottom border-light border-opacity-10">
                            <td className="py-3">
                              <div className="fw-bold">{req.title}</div>
                              <div className="small text-muted text-truncate" style={{maxWidth: '250px'}}>{req.description}</div>
                            </td>
                            <td className="py-3 text-center">
                              <span className={`badge rounded-pill px-3 py-2 ${req.status === 'COMPLETED' ? 'bg-success bg-opacity-25 text-success border border-success' : req.status === 'IN_PROGRESS' ? 'bg-info bg-opacity-25 text-info border border-info' : req.status === 'PENDING_VERIFICATION' ? 'bg-warning bg-opacity-25 text-warning border border-warning' : 'bg-primary bg-opacity-25 text-primary border border-primary'}`}>
                                {req.status}
                              </span>
                            </td>
                            <td className="py-3 text-end">
                              {isVolunteerMode ? (
                                <>
                                  {req.status === 'OPEN' && (
                                    <button onClick={() => handleVerifyAction(req.id, 'accept')} className="btn btn-sm btn-outline-info rounded-pill px-3">Enlist Volunteer</button>
                                  )}
                                  {req.status === 'IN_PROGRESS' && req.volunteer?.id === user.id && (
                                    <button onClick={() => handleVerifyAction(req.id, 'submit')} className="btn btn-sm btn-info rounded-pill px-3 text-dark fw-bold">Submit Validation</button>
                                  )}
                                </>
                              ) : (
                                <>
                                  {req.status === 'PENDING_VERIFICATION' && (
                                    <>
                                      <button onClick={() => handleVerifyAction(req.id, 'complete')} className="btn btn-sm btn-success rounded-pill px-3 me-2">Verify</button>
                                      <button onClick={() => handleVerifyAction(req.id, 'reject')} className="btn btn-sm btn-danger rounded-pill px-3">Intercept</button>
                                    </>
                                  )}
                                </>
                              )}
                            </td>
                          </tr>
                        )) : (
                          <tr><td colSpan="3" className="text-center py-4 text-muted">No secure assignments in this block.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>

                  <h5 className="mb-3 border-top border-secondary pt-4">Workspace Area Map</h5>
                  <div className="w-100 rounded bg-dark border border-light border-opacity-10">
                    <RequestMap requests={displayedRequests} currentUserId={user.id} />
                  </div>
                </>
              ) : (
                  <div className="text-center py-5 text-muted">Join or select a community from Community Network to enable New Request.</div>
              )}
            </div>
          ) : activeTab === 'communities' ? (
            <div className="glass-card h-100">
              <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
                 <div>
                   <h4 className="mb-0">Global Syndicate Architecture</h4>
                 </div>
                 <div className="d-flex gap-2">
                   <button onClick={() => setShowCreateCommModal(true)} className="btn btn-sm btn-info rounded-pill px-3 fw-bold text-dark">
                      <i className="bi bi-plus-lg me-1"></i> New
                   </button>
                   <div style={{width: '180px'}}>
                     <input type="text" className="form-control form-control-sm bg-dark text-light border-secondary rounded-pill" placeholder="Scan matrices..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
                   </div>
                 </div>
              </div>
              <div className="row g-3">
                {communities.map(comm => {
                   const isMember = comm.members?.some(m => sameId(m.id, user.id));
                   const isPending = comm.pendingMembers?.some(m => sameId(m.id, user.id));
                   const isAdmin = sameId(comm.admin?.id, user.id);

                   return (
                     <div key={comm.id} className="col-md-6">
                       <div className="p-3 border border-secondary rounded-4 bg-dark bg-opacity-50 h-100 d-flex flex-column community-card">
                         <div className="d-flex justify-content-between align-items-center mb-2">
                             <div className="d-flex gap-1">
                                {comm.isPrivate && <span className="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2 tiny-badge">PRIVATE</span>}
                                {(isMember || isAdmin) && <span className="badge bg-info bg-opacity-10 text-info border border-info border-opacity-25 px-2 tiny-badge">JOINED</span>}
                             </div>
                         </div>
                         <p className="small text-muted flex-grow-1 mb-3">{comm.description}</p>
                         <div className="d-flex justify-content-between align-items-center mt-auto pt-2 border-top border-secondary border-opacity-25">
                           <span className="small text-muted">{comm.members?.length || 0} Operators</span>
                           <div className="d-flex gap-2">
                               {isAdmin ? (
                                   <button onClick={() => { setActiveAdminComm(comm); setActiveTab('admin_manage'); }} className="btn btn-sm btn-link text-warning p-0 text-decoration-none small fw-bold">ADMIN CONSOLE</button>
                               ) : isPending ? (
                                   <span className="small text-warning fst-italic">PENDING...</span>
                               ) : (
                                   <button onClick={() => handleJoinCommunity(comm.id)} disabled={isMember} className={`btn btn-sm p-0 px-2 small fw-bold ${isMember ? 'text-muted disabled' : 'text-info'}`}>
                                     {isMember ? 'MEMBER' : 'JOIN'}
                                   </button>
                               )}
                           </div>
                         </div>
                       </div>
                     </div>
                   );
                })}
              </div>
            </div>
          ) : activeTab === 'profile' ? (
            <div className="glass-card h-100 p-5">
              <div className="d-flex align-items-center gap-5 mb-5 pb-4 border-bottom border-secondary border-opacity-25">
                <div className="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center text-primary fw-bold p-1 shadow" style={{ width: '120px', height: '120px', fontSize: '3rem', border: '3px solid rgba(59, 130, 246, 0.4)' }}>
                  <div className="bg-dark rounded-circle w-100 h-100 d-flex align-items-center justify-content-center">
                    {user.name.charAt(0).toUpperCase()}
                  </div>
                </div>
                <div>
                  <h2 className="mb-1 text-gradient fw-bold">{user.name}</h2>
                  <p className="text-muted mb-3 fs-5">{user.email || 'id@matrix.assistly'}</p>
                  <div className="d-flex gap-2">
                    <span className="badge rounded-pill bg-primary bg-opacity-25 text-primary border border-primary px-3">{user.role}</span>
                    {user.isVolunteer && <span className="badge rounded-pill bg-success bg-opacity-25 text-success border border-success px-3">ELITE VOLUNTEER</span>}
                  </div>
                </div>
              </div>

              <div className="row g-4 mb-5">
                <div className="col-md-6">
                  <div className="p-4 rounded-4 bg-dark bg-opacity-50 border border-secondary text-center stat-card">
                    <div className="text-muted small mb-2 text-uppercase tracking-wider">Missions Deployed</div>
                    <div className="display-6 fw-bold text-gradient">{profileData?.stats?.requestsPosted || 0}</div>
                  </div>
                </div>
                <div className="col-md-6">
                  <div className="p-4 rounded-4 bg-dark bg-opacity-50 border border-secondary text-center stat-card">
                    <div className="text-muted small mb-2 text-uppercase tracking-wider">Impact Verified</div>
                    <div className="display-6 fw-bold text-info">{profileData?.stats?.requestsCompleted || 0}</div>
                  </div>
                </div>
              </div>

              <h5 className="mb-4 text-uppercase small tracking-widest text-muted fw-bold">Active Badges & Achievements</h5>
              <div className="d-flex flex-wrap gap-3">
                {profileData?.achievements?.length > 0 ? profileData.achievements.map((ach, idx) => (
                  <div key={idx} className="glass-card p-3 px-4 d-flex align-items-center gap-3 achievement-badge" style={{borderRadius: '16px', background: 'rgba(255,255,255,0.02)'}}>
                    <div className="bg-warning bg-opacity-10 text-warning p-2 rounded-circle">
                        <i className="bi bi-patch-check-fill fs-5"></i>
                    </div>
                    <span className="fw-bold small">{ach}</span>
                  </div>
                )) : (
                  <div className="text-center w-100 p-4 border border-dashed border-secondary rounded-4 text-muted small fst-italic">
                    No badges earned yet. Complete missions within the syndicate to unlock achievements.
                  </div>
                )}
              </div>
            </div>
          ) : activeTab === 'admin_manage' && activeAdminComm ? (
            <div className="glass-card h-100 p-4">
              <div className="d-flex justify-content-between align-items-center mb-4 border-bottom border-secondary pb-3">
                 <h4 className="mb-0 text-warning"><i className="bi bi-shield-lock-fill me-2"></i>Admin Console: <span className="text-light">{activeAdminComm.name}</span></h4>
                 <button onClick={() => setActiveTab('communities')} className="btn btn-sm btn-outline-light rounded-pill px-3">Close Console</button>
              </div>

              <h5 className="text-info mb-3">Pending Authorization Queue ({activeAdminComm.pendingMembers?.length || 0})</h5>
              <div className="table-responsive mb-5 border border-secondary rounded">
                  <table className="table table-borderless text-light align-middle mb-0 bg-dark">
                      <tbody>
                          {activeAdminComm.pendingMembers?.length > 0 ? activeAdminComm.pendingMembers.map(m => (
                              <tr key={m.id} className="border-bottom border-secondary">
                                  <td className="ps-3"><div className="fw-bold">{m.name}</div><div className="small text-muted">{m.email}</div></td>
                                  <td className="text-end pe-3">
                                      <button onClick={() => handleAdminAction(activeAdminComm.id, m.id, 'approve')} className="btn btn-sm btn-success rounded-pill px-3 me-2">Authorize</button>
                                      <button onClick={() => handleAdminAction(activeAdminComm.id, m.id, 'reject')} className="btn btn-sm btn-outline-danger rounded-pill px-3">Decline</button>
                                  </td>
                              </tr>
                          )) : <tr><td className="text-center py-4 text-muted fst-italic">Queue is empty.</td></tr>}
                      </tbody>
                  </table>
              </div>

              <h5 className="text-light mb-3">Current Validated Members</h5>
              <div className="table-responsive border border-secondary rounded overflow-hidden">
                  <table className="table table-borderless text-light align-middle mb-0 bg-dark bg-opacity-50">
                      <tbody>
                          {activeAdminComm.members?.map(m => (
                              <tr key={m.id} className="border-bottom border-light border-opacity-10">
                                  <td className="ps-3"><div className="fw-bold">{m.name} {m.id === user.id && <span className="badge bg-warning text-dark ms-2">Admin</span>}</div></td>
                                  <td className="text-end pe-3">
                                      {m.id !== user.id && <button onClick={() => handleAdminAction(activeAdminComm.id, m.id, 'remove')} className="btn btn-sm btn-outline-danger rounded-pill px-3 mt-1">Revoke Access</button>}
                                  </td>
                              </tr>
                          ))}
                      </tbody>
                  </table>
              </div>
            </div>
          ) : null}
        </div>
      </div>

      {showModal && (
        <div className="position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center" style={{backgroundColor: 'rgba(0,0,0,0.85)', zIndex: 1050}}>
          <div className="glass-card animate-fade-in mx-3 p-4 shadow-lg border-primary border-opacity-25" style={{width: '640px'}}>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h4 className="text-gradient mb-0">Initialize Mission Block</h4>
                <div className="badge bg-primary bg-opacity-10 text-primary border border-primary small px-2">ALPHA-LEVEL ACCESS</div>
            </div>
            <form onSubmit={handleCreateRequest}>
              <div className="mb-3">
                <label className="small text-muted mb-1 text-uppercase fw-bold">Directive Title</label>
                <input required type="text" className="form-control bg-dark border-secondary text-light h-100" style={{height: '45px'}} value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} placeholder="Deployment ID..." />
              </div>
              <div className="mb-3">
                <label className="small text-muted mb-1 text-uppercase fw-bold">Payload Description</label>
                <textarea required className="form-control bg-dark border-secondary text-light" rows="3" value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} placeholder="Detailed instructions for the field operatives..."></textarea>
              </div>
              <div className="mb-3">
                <label className="small text-muted mb-2 text-uppercase fw-bold">Target Coordinates (Click to Pin)</label>
                <div className="rounded-4 border border-secondary overflow-hidden position-relative" style={{height: '240px'}}>
                  <RequestMap 
                    onLocationSelect={(lat, lng) => setFormData({...formData, latitude: lat, longitude: lng})} 
                    selectedLocation={{lat: formData.latitude, lng: formData.longitude}}
                    requests={[]}
                  />
                  <div className="position-absolute bottom-0 start-0 w-100 bg-dark bg-opacity-75 p-2 px-3 small border-top border-secondary">
                      <span className="text-muted">LAT:</span> <span className="text-info mono">{formData.latitude.toFixed(6)}</span> 
                      <span className="text-muted ms-3">LNG:</span> <span className="text-info mono">{formData.longitude.toFixed(6)}</span>
                  </div>
                </div>
              </div>
              <div className="d-flex justify-content-end gap-3 mt-4 pt-4 border-top border-secondary border-opacity-25">
                <button type="button" onClick={() => setShowModal(false)} className="btn btn-link text-muted text-decoration-none px-4">ABORT</button>
                <button type="submit" disabled={loading} className="neon-button px-5">{loading ? 'TRANSMITTING...' : 'BROADCAST MISSION'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showCreateCommModal && (
        <div className="position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center" style={{backgroundColor: 'rgba(0,0,0,0.85)', zIndex: 1050}}>
          <div className="glass-card animate-fade-in mx-3 p-4 shadow-lg border-info border-opacity-25" style={{width: '500px'}}>
            <h4 className="text-gradient mb-4">Establish New Syndicate Community</h4>
            <form onSubmit={handleCreateCommunity}>
              <div className="mb-3">
                <label className="small text-muted mb-1 text-uppercase fw-bold">Syndicate Name</label>
                <input required type="text" className="form-control bg-dark border-secondary text-light" value={commFormData.name} onChange={e => setCommFormData({...commFormData, name: e.target.value})} placeholder="Community Designation..." />
              </div>
              <div className="mb-3">
                <label className="small text-muted mb-1 text-uppercase fw-bold">Directives & Purpose</label>
                <textarea required className="form-control bg-dark border-secondary text-light" rows="3" value={commFormData.description} onChange={e => setCommFormData({...commFormData, description: e.target.value})} placeholder="Mission goals and community guidelines..."></textarea>
              </div>
              <div className="form-check form-switch mb-4 mt-3">
                <input className="form-check-input" type="checkbox" id="privateSwitch" checked={commFormData.isPrivate} onChange={e => setCommFormData({...commFormData, isPrivate: e.target.checked})} style={{cursor: 'pointer'}} />
                <label className="form-check-label text-muted ms-2 small fw-bold" htmlFor="privateSwitch">ENCRYPTED ACCESS (Requires Authorization)</label>
              </div>
              <div className="d-flex justify-content-end gap-3 mt-4 pt-3 border-top border-secondary border-opacity-25">
                <button type="button" onClick={() => setShowCreateCommModal(false)} className="btn btn-link text-muted text-decoration-none px-4">CANCEL</button>
                <button type="submit" disabled={loading} className="btn btn-info rounded-pill px-5 fw-bold text-dark shadow-sm">{loading ? 'ESTABLISHING...' : 'ESTABLISH SYNDICATE'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserDashboard;
