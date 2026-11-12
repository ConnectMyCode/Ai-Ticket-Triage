import React, { useEffect,useState } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

const API_BASE_URL = 'http://13.203.220.149:8080';

function App() {
    const [token, setToken] = useState(
        () => sessionStorage.getItem('jwtToken')
    );

    const [page, setPage] = useState('login');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);
    const [tickets, setTickets] = useState([]);
    const [ticketPage, setTicketPage] = useState(0);
    const [hasMoreTickets, setHasMoreTickets] = useState(false);
    const [loadingMoreTickets, setLoadingMoreTickets] = useState(false);
    const [ticketCounts, setTicketCounts] = useState({
        total: 0,
        open: 0,
        inProgress: 0,
        closed: 0
    });
    const [dashboardLoading, setDashboardLoading] = useState(false);
    const [dashboardError, setDashboardError] = useState('');
    const [createTitle, setCreateTitle] = useState('');
    const [createDescription, setCreateDescription] = useState('');
    const [createPriority, setCreatePriority] = useState('LOW');
    const [createTicketMessage, setCreateTicketMessage] = useState('');
    const [createTicketError, setCreateTicketError] = useState('');
    const [creatingTicket, setCreatingTicket] = useState(false);
    const [ticketStatusSelections, setTicketStatusSelections] = useState({});
    const [updatingTicketId, setUpdatingTicketId] = useState(null);
    const [ticketStatusFeedback, setTicketStatusFeedback] = useState({});
    const [closingTicketId, setClosingTicketId] = useState(null);
    const [closeTicketFeedback, setCloseTicketFeedback] = useState({});
    const [ticketStatusFilter, setTicketStatusFilter] = useState('');
    const [ticketPriorityFilter, setTicketPriorityFilter] = useState('');
    const [applyingTicketFilters, setApplyingTicketFilters] = useState(false);

    // AI Triage page state
    const [triageTickets, setTriageTickets] = useState([]);
    const [triageTicketsLoading, setTriageTicketsLoading] = useState(false);
    const [triageTicketsError, setTriageTicketsError] = useState('');
    const [selectedTriageTicketId, setSelectedTriageTicketId] = useState('');
    const [triageResult, setTriageResult] = useState(null);
    const [triageLoading, setTriageLoading] = useState(false);
    const [triageSaving, setTriageSaving] = useState(false);
    const [triageMessage, setTriageMessage] = useState('');
    const [triageError, setTriageError] = useState('');
/*************************************************************************************************************************/


    useEffect(() => {
        if (!token) {
            return;
        }

        async function loadDashboard() {
            setDashboardLoading(true);
            setDashboardError('');

            try {
                const [
                    allTickets,
                    openTickets,
                    inProgressTickets,
                    closedTickets
                ] = await Promise.all([
                    fetchTicketPage(),
                    fetchTicketPage('OPEN'),
                    fetchTicketPage('IN_PROGRESS'),
                    fetchTicketPage('CLOSED')
                ]);

                setTickets(allTickets.content || []);
                setTicketPage(0);
                setHasMoreTickets(
                    (allTickets.number || 0) < (allTickets.totalPages || 1) - 1
                );

                setTicketCounts({
                    total: allTickets.totalElements || 0,
                    open: openTickets.totalElements || 0,
                    inProgress: inProgressTickets.totalElements || 0,
                    closed: closedTickets.totalElements || 0
                });

            } catch (error) {
                setDashboardError(error.message);
            } finally {
                setDashboardLoading(false);
            }
        }

        loadDashboard();
    }, [token]);



/*************************************************************************************************************************/


    async function handleRegister(event) {
        event.preventDefault();
        setMessage('');

        if (password !== confirmPassword) {
            setMessage('Passwords do not match.');
            return;
        }

        setLoading(true);

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/signup`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                const errorText = await response.text();
                let errorMessage = 'Registration failed.';

                try {
                    const errorData = JSON.parse(errorText);
                    errorMessage = errorData.message || errorMessage;
                } catch {
                    errorMessage = errorText || errorMessage;
                }

                setMessage(errorMessage);
                return;
            }

            setEmail('');
            setPassword('');
            setConfirmPassword('');
            setMessage('Registration successful. Please log in.');
            setPage('login');

        } catch {
            setMessage('Unable to connect to the backend. Please try again.');
        } finally {
            setLoading(false);
        }
    }


/*************************************************************************************************************************/

    async function handleLogin(event) {
        event.preventDefault();
        setMessage('');
        setLoading(true);

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();

            if (!response.ok) {
                setMessage(data.message || 'Login failed.');
                return;
            }

            if (!data.token) {
                setMessage('Login response did not contain a JWT token.');
                return;
            }

            sessionStorage.setItem('jwtToken', data.token);
            setToken(data.token);
            setEmail('');
            setPassword('');
            setPage('dashboard');

        } catch {
            setMessage('Unable to connect to the backend. Please try again.');
        } finally {
            setLoading(false);
        }
    }

/*************************************************************************************************************************/


    function handleLogout() {
        sessionStorage.removeItem('jwtToken');
        setToken(null);
        setPage('login');
        setMessage('You have been logged out.');
    }


/*************************************************************************************************************************/


    async function fetchTicketPage(
        status = '',
        pageNumber = 0,
        pageSize = status ? 1 : 5,
        priority = ''
    ) {
        const params = new URLSearchParams({
            page: String(pageNumber),
            size: String(pageSize)
        });

        if (status) {
            params.append('status', status);
        }

        if (priority) {
            params.append('priority', priority);
        }

        const response = await fetch(
            `${API_BASE_URL}/api/tickets?${params.toString()}`,
            {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Ticket API failed with status ${response.status}`);
        }
        return await response.json();
    }

/*------------------------------------*/

    // Reload the dashboard after a ticket is created.
    async function refreshDashboard() {
        const [
            allTickets,
            openTickets,
            inProgressTickets,
            closedTickets
        ] = await Promise.all([
            fetchTicketPage(ticketStatusFilter, 0, 5, ticketPriorityFilter),
            fetchTicketPage('OPEN'),
            fetchTicketPage('IN_PROGRESS'),
            fetchTicketPage('CLOSED')
        ]);

        setTickets(allTickets.content || []);
        setTicketPage(0);
        setHasMoreTickets(
            (allTickets.number || 0) < (allTickets.totalPages || 1) - 1
        );

        setTicketCounts({
            total: allTickets.totalElements || 0,
            open: openTickets.totalElements || 0,
            inProgress: inProgressTickets.totalElements || 0,
            closed: closedTickets.totalElements || 0
        });
        setDashboardError('');
    }

    async function handleCreateTicket(event) {
        event.preventDefault();
        setCreateTicketMessage('');
        setCreateTicketError('');

        const trimmedTitle = createTitle.trim();
        const trimmedDescription = createDescription.trim();

        if (!trimmedTitle || !trimmedDescription) {
            setCreateTicketError('Please enter both a title and a description.');
            return;
        }

        setCreatingTicket(true);

        try {
            const response = await fetch(`${API_BASE_URL}/api/tickets`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    title: trimmedTitle,
                    description: trimmedDescription,
                    priority: createPriority
                })
            });

            if (!response.ok) {
                const responseText = await response.text();
                let errorMessage = 'Unable to create the ticket. Please try again.';

                if (responseText) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorMessage;
                    } catch {
                        errorMessage = responseText;
                    }
                }

                setCreateTicketError(errorMessage);
                return;
            }

            // The create endpoint returns HTTP 200 with an empty response body.
            // Do not call response.json() here.
            setCreateTitle('');
            setCreateDescription('');
            setCreatePriority('LOW');

            await refreshDashboard();
            setCreateTicketMessage('Ticket created successfully.');
        } catch {
            setCreateTicketError(
                'Unable to connect to the backend. Please try again.'
            );
        } finally {
            setCreatingTicket(false);
        }
    }

    async function handleUpdateTicketStatus(ticketId, currentStatus) {
        const selectedStatus = ticketStatusSelections[ticketId] || currentStatus;

        if (selectedStatus === currentStatus) {
            setTicketStatusFeedback((previousFeedback) => ({
                ...previousFeedback,
                [ticketId]: 'Select a different status before updating.'
            }));
            return;
        }

        setUpdatingTicketId(ticketId);
        setTicketStatusFeedback((previousFeedback) => ({
            ...previousFeedback,
            [ticketId]: ''
        }));

        try {
            const response = await fetch(`${API_BASE_URL}/api/tickets/${ticketId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ status: selectedStatus })
            });

            if (!response.ok) {
                const responseText = await response.text();
                let errorMessage = 'Unable to update ticket status. Please try again.';

                if (responseText) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } catch {
                        errorMessage = responseText;
                    }
                }

                setTicketStatusFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: errorMessage
                }));
                return;
            }

            // The status endpoint returns an empty response body.
            // Do not call response.json() here.
            try {
                await refreshDashboard();
                setTicketStatusSelections((previousSelections) => ({
                    ...previousSelections,
                    [ticketId]: selectedStatus
                }));
                setTicketStatusFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: 'Ticket status updated successfully.'
                }));
            } catch {
                setTicketStatusFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: 'Status updated, but the dashboard could not refresh. Please reload the page.'
                }));
            }
        } catch {
            setTicketStatusFeedback((previousFeedback) => ({
                ...previousFeedback,
                [ticketId]: 'Unable to connect to the backend. Please try again.'
            }));
        } finally {
            setUpdatingTicketId(null);
        }
    }

    async function handleCloseTicket(ticketId, currentStatus) {
        if (currentStatus === 'CLOSED') {
            setCloseTicketFeedback((previousFeedback) => ({
                ...previousFeedback,
                [ticketId]: 'This ticket is already closed.'
            }));
            return;
        }

        setClosingTicketId(ticketId);
        setCloseTicketFeedback((previousFeedback) => ({
            ...previousFeedback,
            [ticketId]: ''
        }));

        try {
            const response = await fetch(`${API_BASE_URL}/api/tickets/${ticketId}/close`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const responseText = await response.text();
                let errorMessage = 'Unable to close ticket. Please try again.';

                if (responseText) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } catch {
                        errorMessage = responseText;
                    }
                }

                setCloseTicketFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: errorMessage
                }));
                return;
            }

            // The close endpoint returns an empty response body.
            // Do not call response.json() here.
            try {
                await refreshDashboard();
                setCloseTicketFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: 'Ticket closed successfully.'
                }));
            } catch {
                setCloseTicketFeedback((previousFeedback) => ({
                    ...previousFeedback,
                    [ticketId]: 'Ticket closed, but the dashboard could not refresh. Please reload the page.'
                }));
            }
        } catch {
            setCloseTicketFeedback((previousFeedback) => ({
                ...previousFeedback,
                [ticketId]: 'Unable to connect to the backend. Please try again.'
            }));
        } finally {
            setClosingTicketId(null);
        }
    }


    async function loadTriageTickets() {
        setTriageTicketsLoading(true);
        setTriageTicketsError('');
        setTriageError('');
        setTriageMessage('');
        setTriageResult(null);
        setSelectedTriageTicketId('');

        try {
            // This endpoint returns only tickets belonging to the authenticated user.
            const firstPage = await fetchTicketPage('', 0, 100);
            let allUserTickets = firstPage.content || [];

            // Load any remaining pages so the page can list all of this user's tickets.
            for (let pageNumber = 1; pageNumber < (firstPage.totalPages || 1); pageNumber++) {
                const nextPage = await fetchTicketPage('', pageNumber, 100);
                allUserTickets = [...allUserTickets, ...(nextPage.content || [])];
            }

            setTriageTickets(allUserTickets);
        } catch (error) {
            setTriageTicketsError(error.message);
        } finally {
            setTriageTicketsLoading(false);
        }
    }

    async function handleRunAiTriage() {
        if (!selectedTriageTicketId) {
            setTriageError('Please select a ticket first.');
            return;
        }

        setTriageLoading(true);
        setTriageError('');
        setTriageMessage('');
        setTriageResult(null);

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/tickets/${selectedTriageTicketId}/triage/run`,
                {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'application/json'
                    }
                }
            );

            const responseText = await response.text();
            let responseData = {};

            if (responseText) {
                try {
                    responseData = JSON.parse(responseText);
                } catch {
                    responseData = { message: responseText };
                }
            }

            if (!response.ok) {
                throw new Error(responseData.message || responseData.error || 'Unable to run AI triage.');
            }

            setTriageResult(responseData);
        } catch (error) {
            setTriageError(error.message || 'Unable to connect to the backend.');
        } finally {
            setTriageLoading(false);
        }
    }

    async function handleSaveTriage() {
        if (!triageResult || !selectedTriageTicketId) {
            return;
        }

        setTriageSaving(true);
        setTriageError('');
        setTriageMessage('');

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/tickets/${selectedTriageTicketId}/triage/save`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'application/json'
                    },
                    // The save DTO does not include tags.
                    body: JSON.stringify({
                        category: triageResult.category,
                        sentiment: triageResult.sentiment,
                        priority: triageResult.priority,
                        summary: triageResult.summary,
                        replyDraft: triageResult.replyDraft
                    })
                }
            );

            const responseText = await response.text();
            if (!response.ok) {
                let errorMessage = 'Unable to save triage result.';
                if (responseText) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorMessage = errorData.message || errorData.error || errorMessage;
                    } catch {
                        errorMessage = responseText;
                    }
                }
                throw new Error(errorMessage);
            }

            // Save succeeds with an empty response body.
            setTriageMessage('Triage result saved successfully.');
            setTriageResult(null);
        } catch (error) {
            setTriageError(error.message || 'Unable to connect to the backend.');
        } finally {
            setTriageSaving(false);
        }
    }

    function handleRejectTriage() {
        // Reject is local only; there is no backend delete endpoint.
        setTriageResult(null);
        setTriageError('');
        setTriageMessage('Triage result rejected. Nothing was saved.');
    }

    async function handleApplyTicketFilters() {
        setApplyingTicketFilters(true);
        setDashboardError('');

        try {
            const filteredTickets = await fetchTicketPage(
                ticketStatusFilter,
                0,
                5,
                ticketPriorityFilter
            );

            setTickets(filteredTickets.content || []);
            setTicketPage(0);
            setHasMoreTickets(
                (filteredTickets.number || 0) < (filteredTickets.totalPages || 1) - 1
            );
        } catch (error) {
            setDashboardError(error.message);
        } finally {
            setApplyingTicketFilters(false);
        }
    }

    async function handleClearTicketFilters() {
        setTicketStatusFilter('');
        setTicketPriorityFilter('');
        setApplyingTicketFilters(true);
        setDashboardError('');

        try {
            const firstPage = await fetchTicketPage('', 0, 5);
            setTickets(firstPage.content || []);
            setTicketPage(0);
            setHasMoreTickets(
                (firstPage.number || 0) < (firstPage.totalPages || 1) - 1
            );
        } catch (error) {
            setDashboardError(error.message);
        } finally {
            setApplyingTicketFilters(false);
        }
    }

    async function handleLoadMoreTickets() {
        if (loadingMoreTickets || !hasMoreTickets) {
            return;
        }

        const nextPage = ticketPage + 1;

        setLoadingMoreTickets(true);

        try {
            const nextTicketPage = await fetchTicketPage(ticketStatusFilter, nextPage, 5, ticketPriorityFilter);

            setTickets((previousTickets) => [
                ...previousTickets,
                ...(nextTicketPage.content || [])
            ]);

            setTicketPage(nextPage);

            setHasMoreTickets(
                nextPage < (nextTicketPage.totalPages || 1) - 1
            );

        } catch (error) {
            setDashboardError(error.message);
        } finally {
            setLoadingMoreTickets(false);
        }
    }


/*************************************************************************************************************************/


    if (!token) {
        return (
            <div className="auth-page">
                <div className="auth-card">
                    <div className="auth-brand">
                        <div className="brand-icon">T</div>
                        <h1>Ticket Triage</h1>
                    </div>

                    {page === 'register' ? (
                        <>
                            <h2>Create your account</h2>
                            <p className="auth-description">
                                Register to start managing support tickets.
                            </p>

                            <form onSubmit={handleRegister}>
                                <label>Email address</label>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(event) => setEmail(event.target.value)}
                                    placeholder="Enter your email"
                                    required
                                />

                                <label>Password</label>
                                <input
                                    type="password"
                                    value={password}
                                    onChange={(event) => setPassword(event.target.value)}
                                    placeholder="Minimum 8 characters"
                                    minLength={8}
                                    required
                                />

                                <label>Confirm password</label>
                                <input
                                    type="password"
                                    value={confirmPassword}
                                    onChange={(event) => setConfirmPassword(event.target.value)}
                                    placeholder="Confirm your password"
                                    minLength={8}
                                    required
                                />

                                <button className="primary-button auth-button" disabled={loading}>
                                    {loading ? 'Registering...' : 'Create Account'}
                                </button>
                            </form>

                            <p className="auth-switch">
                                Already have an account?{' '}
                                <button type="button" onClick={() => {
                                    setMessage('');
                                    setPage('login');
                                }}>
                                    Login
                                </button>
                            </p>
                        </>
                    ) : (
                        <>
                            <h2>Welcome back</h2>
                            <p className="auth-description">
                                Login to access your support ticket workspace.
                            </p>

                            <form onSubmit={handleLogin}>
                                <label>Email address</label>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(event) => setEmail(event.target.value)}
                                    placeholder="Enter your email"
                                    required
                                />

                                <label>Password</label>
                                <input
                                    type="password"
                                    value={password}
                                    onChange={(event) => setPassword(event.target.value)}
                                    placeholder="Enter your password"
                                    required
                                />

                                <button className="primary-button auth-button" disabled={loading}>
                                    {loading ? 'Logging in...' : 'Login'}
                                </button>
                            </form>

                            <p className="auth-switch">
                                Don't have an account?{' '}
                                <button type="button" onClick={() => {
                                    setMessage('');
                                    setPage('register');
                                }}>
                                    Register
                                </button>
                            </p>
                        </>
                    )}

                    {message && (
                        <p className="auth-message" role="status">
                            {message}
                        </p>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="app">
            <aside className="sidebar">
                <div className="brand">
                    <div className="brand-icon">T</div>
                    <span>Ticket Triage</span>
                </div>

                <nav className="sidebar-nav">
                    <p className="nav-label">WORKSPACE</p>

                    <button
                        className={`nav-item ${page === 'dashboard' ? 'active' : ''}`}
                        type="button"
                        onClick={() => setPage('dashboard')}
                    >
                        <span>▦</span> Dashboard
                    </button>

                    <button className="nav-item" type="button">
                        <span>▤</span> My Tickets
                    </button>

                    <button
                        className={`nav-item ${page === 'create-ticket' ? 'active' : ''}`}
                        type="button"
                        onClick={() => {
                            setCreateTicketMessage('');
                            setCreateTicketError('');
                            setPage('create-ticket');
                        }}
                    >
                        <span>＋</span> Create Ticket
                    </button>

                    <button
                        className={`nav-item ${page === 'ai-triage' ? 'active' : ''}`}
                        type="button"
                        onClick={() => {
                            setPage('ai-triage');
                            loadTriageTickets();
                        }}
                    >
                        <span>✦</span> AI Triage
                    </button>
                </nav>

                <div className="sidebar-footer">
                    <div className="avatar">U</div>
                    <div>
                        <strong>Authenticated User</strong>
                        <p>Signed in</p>
                    </div>
                    <button className="logout-button" onClick={handleLogout}>
                        Logout
                    </button>
                </div>
            </aside>

            <main className="main-content">
                <header className="topbar">
                    <div>
                        <p className="breadcrumb">Workspace / {page === 'ai-triage' ? 'AI Triage' : page === 'create-ticket' ? 'Create Ticket' : 'Dashboard'}</p>
                        <h1>{page === 'ai-triage' ? 'AI Triage' : page === 'create-ticket' ? 'Create Ticket' : 'Dashboard'}</h1>
                    </div>
                    <span className="environment-label">Frontend Preview</span>
                </header>

                {page === 'ai-triage' ? (
                    <section className="tickets-panel ai-triage-panel">
                        <div className="panel-header">
                            <div>
                                <h2>Select a Ticket</h2>
                                <p>Choose one of your tickets to generate an AI triage suggestion.</p>
                            </div>
                            <button className="secondary-button" type="button" onClick={loadTriageTickets} disabled={triageTicketsLoading}>
                                {triageTicketsLoading ? 'Refreshing...' : 'Refresh Tickets'}
                            </button>
                        </div>

                        {triageTicketsLoading && <p className="dashboard-message">Loading your tickets...</p>}
                        {triageTicketsError && <p className="dashboard-error" role="alert">{triageTicketsError}</p>}

                        {!triageTicketsLoading && !triageTicketsError && triageTickets.length === 0 && (
                            <div className="empty-state">
                                <div className="empty-icon">▤</div>
                                <h3>No tickets found</h3>
                                <p>Your tickets will appear here after you create them.</p>
                            </div>
                        )}

                        {!triageTicketsLoading && triageTickets.length > 0 && (
                            <div className="ai-triage-ticket-list">
                                {triageTickets.map((ticket) => (
                                    <label className={`ai-triage-ticket-option ${String(selectedTriageTicketId) === String(ticket.id) ? 'selected' : ''}`} key={ticket.id}>
                                        <input
                                            type="radio"
                                            name="selected-triage-ticket"
                                            value={ticket.id}
                                            checked={String(selectedTriageTicketId) === String(ticket.id)}
                                            onChange={() => {
                                                setSelectedTriageTicketId(String(ticket.id));
                                                setTriageResult(null);
                                                setTriageError('');
                                                setTriageMessage('');
                                            }}
                                        />
                                        <span>
                                            <strong>#{ticket.id} — {ticket.title}</strong>
                                            <small>{ticket.status} · {ticket.priority}</small>
                                            <small>{ticket.description || 'No description available.'}</small>
                                        </span>
                                    </label>
                                ))}
                            </div>
                        )}

                        {selectedTriageTicketId && (
                            <div className="ai-triage-actions">
                                <button className="primary-button" type="button" onClick={handleRunAiTriage} disabled={triageLoading || triageSaving}>
                                    {triageLoading ? 'Running AI Triage...' : 'Run AI Triage'}
                                </button>
                            </div>
                        )}

                        {triageError && <p className="create-ticket-error" role="alert">{triageError}</p>}
                        {triageMessage && <p className="create-ticket-success" role="status">{triageMessage}</p>}

                        {triageResult && (
                            <section className="ai-triage-result">
                                <h2>AI Triage Result</h2>
                                <p><strong>Category:</strong> {triageResult.category}</p>
                                <p><strong>Suggested priority:</strong> {triageResult.priority}</p>
                                <p><strong>Sentiment:</strong> {triageResult.sentiment}</p>
                                <p><strong>Tags:</strong> {(triageResult.tags || []).join(', ') || 'None'}</p>
                                <p><strong>Summary:</strong> {triageResult.summary}</p>
                                <div>
                                    <strong>Reply draft:</strong>
                                    <p>{triageResult.replyDraft}</p>
                                </div>
                                <div className="ai-triage-actions">
                                    <button className="primary-button" type="button" onClick={handleSaveTriage} disabled={triageSaving}>
                                        {triageSaving ? 'Saving...' : 'Save Triage'}
                                    </button>
                                    <button className="secondary-button" type="button" onClick={handleRejectTriage} disabled={triageSaving}>
                                        Reject
                                    </button>
                                </div>
                            </section>
                        )}
                    </section>
                ) : page === 'create-ticket' ? (
                    <section className="create-ticket-panel">
                        <div className="panel-header">
                            <div>
                                <h2>Create a Ticket</h2>
                                <p>Describe the issue you need help with.</p>
                            </div>
                        </div>

                        <form className="create-ticket-form" onSubmit={handleCreateTicket}>
                            <label htmlFor="ticket-title">Title</label>
                            <input
                                id="ticket-title"
                                type="text"
                                value={createTitle}
                                onChange={(event) => setCreateTitle(event.target.value)}
                                placeholder="Enter a short title"
                                required
                            />

                            <label htmlFor="ticket-description">Description</label>
                            <textarea
                                id="ticket-description"
                                value={createDescription}
                                onChange={(event) => setCreateDescription(event.target.value)}
                                placeholder="Describe the issue in detail"
                                rows={6}
                                required
                            />

                            <label htmlFor="ticket-priority">Priority</label>
                            <select
                                id="ticket-priority"
                                value={createPriority}
                                onChange={(event) => setCreatePriority(event.target.value)}
                            >
                                <option value="LOW">Low</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HIGH">High</option>
                            </select>

                            {createTicketError && (
                                <p className="create-ticket-error" role="alert">
                                    {createTicketError}
                                </p>
                            )}

                            {createTicketMessage && (
                                <p className="create-ticket-success" role="status">
                                    {createTicketMessage}
                                </p>
                            )}

                            <div className="create-ticket-actions">
                                <button
                                    className="secondary-button"
                                    type="button"
                                    onClick={() => {
                                        setCreateTicketError('');
                                        setCreateTicketMessage('');
                                        setPage('dashboard');
                                    }}
                                    disabled={creatingTicket}
                                >
                                    Cancel
                                </button>
                                <button
                                    className="primary-button"
                                    type="submit"
                                    disabled={creatingTicket}
                                >
                                    {creatingTicket ? 'Creating...' : 'Create Ticket'}
                                </button>
                            </div>
                        </form>
                    </section>
                ) : (
                        <>

                                    <section className="welcome-section">
                                        <h2>Welcome to Ticket Triage</h2>
                                        <p>
                                            Manage support tickets, track their progress, and use
                                            AI-assisted triage to organize incoming issues.
                                        </p>
                                    </section>
                    
                                    <section className="stats-grid">
                                        {[
                                            { label: 'Total Tickets', value: ticketCounts.total },
                                            { label: 'Open Tickets', value: ticketCounts.open },
                                            { label: 'In Progress', value: ticketCounts.inProgress },
                                            { label: 'Closed Tickets', value: ticketCounts.closed }
                                        ].map((stat) => (
                                            <article className="stat-card" key={stat.label}>
                                                <p>{stat.label}</p>
                                                <h3>
                                                    {dashboardLoading ? '...' : stat.value}
                                                </h3>
                                                <span>Live ticket data</span>
                                            </article>
                                        ))}
                                        </section>
                    
                                    <section className="tickets-panel">
                                        <div className="panel-header">
                                            <div>
                                                <h2>Recent Tickets</h2>
                                                <p>Your latest support tickets.</p>
                                            </div>
                                        </div>

                                        <div className="ticket-filters">
                                            <label htmlFor="ticket-status-filter">Status</label>
                                            <select
                                                id="ticket-status-filter"
                                                value={ticketStatusFilter}
                                                onChange={(event) => setTicketStatusFilter(event.target.value)}
                                            >
                                                <option value="">All statuses</option>
                                                <option value="OPEN">Open</option>
                                                <option value="IN_PROGRESS">In Progress</option>
                                                <option value="CLOSED">Closed</option>
                                            </select>

                                            <label htmlFor="ticket-priority-filter">Priority</label>
                                            <select
                                                id="ticket-priority-filter"
                                                value={ticketPriorityFilter}
                                                onChange={(event) => setTicketPriorityFilter(event.target.value)}
                                            >
                                                <option value="">All priorities</option>
                                                <option value="LOW">Low</option>
                                                <option value="MEDIUM">Medium</option>
                                                <option value="HIGH">High</option>
                                            </select>

                                            <button
                                                className="ticket-filter-button"
                                                type="button"
                                                onClick={handleApplyTicketFilters}
                                                disabled={applyingTicketFilters}
                                            >
                                                {applyingTicketFilters ? 'Applying...' : 'Apply Filters'}
                                            </button>
                                            <button
                                                className="ticket-filter-clear-button"
                                                type="button"
                                                onClick={handleClearTicketFilters}
                                                disabled={applyingTicketFilters}
                                            >
                                                Clear
                                            </button>
                                        </div>


                                        {dashboardLoading && (
                                            <p className="dashboard-message">Loading tickets...</p>
                                        )}
                    
                                        {dashboardError && (
                                            <p className="dashboard-error">{dashboardError}</p>
                                        )}
                    
                                        {!dashboardLoading && !dashboardError && tickets.length === 0 && (
                                            <div className="empty-state">
                                                <div className="empty-icon">▤</div>
                                                <h3>No tickets found</h3>
                                                <p>Your support tickets will appear here.</p>
                                            </div>
                                        )}
                    
                                        {!dashboardLoading && !dashboardError && tickets.length > 0 && (
                                            <>
                                                <div className="ticket-grid">
                                                    {tickets.map((ticket) => (
                                                        <article className="ticket-card" key={ticket.id}>
                    
                                                            <div className="ticket-card-header">
                                                                <h3>#{ticket.id}</h3>
                    
                                                                <span className="ticket-date">
                                                {ticket.createdAt
                                                    ? new Date(ticket.createdAt).toLocaleDateString()
                                                    : 'Date unavailable'}
                                            </span>
                                                            </div>
                    
                                                            <div className="ticket-card-body">
                                                                <h4>{ticket.title}</h4>
                    
                                                                <p>
                                                                    {ticket.description || 'No description available.'}
                                                                </p>
                                                            </div>
                    
                                                            <div className="ticket-card-footer">
                                            <span className={`ticket-status status-${(ticket.status || '').toLowerCase()}`}>
                                                {ticket.status}
                                            </span>
                    
                                                                <span className={`ticket-priority priority-${(ticket.priority || '').toLowerCase()}`}>
                                                {ticket.priority}
                                            </span>
                                                            </div>

                                                            <div className="ticket-status-actions">
                                                                <select
                                                                    value={ticketStatusSelections[ticket.id] || ticket.status}
                                                                    onChange={(event) => {
                                                                        setTicketStatusSelections((previousSelections) => ({
                                                                            ...previousSelections,
                                                                            [ticket.id]: event.target.value
                                                                        }));
                                                                        setTicketStatusFeedback((previousFeedback) => ({
                                                                            ...previousFeedback,
                                                                            [ticket.id]: ''
                                                                        }));
                                                                    }}
                                                                    disabled={updatingTicketId === ticket.id || ticket.status === 'CLOSED'}
                                                                    aria-label={`Change status for ticket ${ticket.id}`}
                                                                >
                                                                    <option value="OPEN">Open</option>
                                                                    <option value="IN_PROGRESS">In Progress</option>
                                                                    <option value="CLOSED">Closed</option>
                                                                </select>
                                                                <button
                                                                    className="ticket-status-update-button"
                                                                    type="button"
                                                                    onClick={() => handleUpdateTicketStatus(ticket.id, ticket.status)}
                                                                    disabled={updatingTicketId === ticket.id || ticket.status === 'CLOSED'}
                                                                >
                                                                    {updatingTicketId === ticket.id ? 'Updating...' : 'Update Status'}
                                                                </button>
                                                            </div>

                                                            {ticketStatusFeedback[ticket.id] && (
                                                                <p className="ticket-status-feedback" role="status">
                                                                    {ticketStatusFeedback[ticket.id]}
                                                                </p>
                                                            )}

                                                            <div className="ticket-close-actions">
                                                                <button
                                                                    className="ticket-close-button"
                                                                    type="button"
                                                                    onClick={() => handleCloseTicket(ticket.id, ticket.status)}
                                                                    disabled={closingTicketId === ticket.id || ticket.status === 'CLOSED'}
                                                                >
                                                                    {closingTicketId === ticket.id
                                                                        ? 'Closing...'
                                                                        : ticket.status === 'CLOSED'
                                                                            ? 'Ticket Closed'
                                                                            : 'Close Ticket'}
                                                                </button>
                                                            </div>

                                                            {closeTicketFeedback[ticket.id] && (
                                                                <p className="ticket-close-feedback" role="status">
                                                                    {closeTicketFeedback[ticket.id]}
                                                                </p>
                                                            )}

                                                        </article>
                                                    ))}
                                                </div>
                    
                                                {hasMoreTickets && (
                                                    <div className="load-more-container">
                                                        <button
                                                            className="load-more-button"
                                                            type="button"
                                                            onClick={handleLoadMoreTickets}
                                                            disabled={loadingMoreTickets}
                                                        >
                                                            {loadingMoreTickets
                                                                ? 'Loading...'
                                                                : 'Load More Tickets'}
                                                        </button>
                                                    </div>
                                                )}
                                            </>
                                        )}
                    
                                    </section>
                        </>
                )}
            </main>
        </div>
    );
}

createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);