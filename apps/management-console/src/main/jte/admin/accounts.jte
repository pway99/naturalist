@import com.naturalist.account.Account
@import com.naturalist.account.AccessLevel
@import com.naturalist.account.AccountStatus
@import com.naturalist.data.Page

@param Page<Account> accounts
@param String notice
@param org.springframework.security.web.csrf.CsrfToken _csrf = null

@template.layout.page(title = "Accounts", content = @`
    @template.admin.nav(current = "accounts")
    <section class="admin-accounts">
        <h1>Accounts</h1>
        <p>Every registered account. Grant or revoke vision identification, and suspend or
            reinstate an account. Suspending also signs the account out everywhere.</p>

        @if(!notice.isEmpty())
            <p class="admin-accounts-notice" role="status">${notice}</p>
        @endif

        @if(accounts.content().isEmpty())
            <p><em>No accounts on this page.</em></p>
        @else
            <table class="admin-accounts-table">
                <thead>
                    <tr>
                        <th scope="col">Email</th>
                        <th scope="col">Verified</th>
                        <th scope="col">Access</th>
                        <th scope="col">Status</th>
                        <th scope="col">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    @for(Account account : accounts.content())
                        <tr>
                            <td>${account.email()}</td>
                            <td>${account.emailVerified() ? "yes" : "no"}</td>
                            <td>${account.access() == AccessLevel.VISION ? "vision" : "browse only"}</td>
                            <td>${account.status() == AccountStatus.SUSPENDED ? "suspended" : "active"}</td>
                            <td class="admin-accounts-actions">
                                @if(account.access() == AccessLevel.VISION)
                                    @template.admin.accountAction(account = account, action = "revoke-vision", label = "Revoke vision", page = accounts.pageNumber(), _csrf = _csrf)
                                @else
                                    @template.admin.accountAction(account = account, action = "grant-vision", label = "Grant vision", page = accounts.pageNumber(), _csrf = _csrf)
                                @endif
                                @if(account.status() == AccountStatus.SUSPENDED)
                                    @template.admin.accountAction(account = account, action = "reinstate", label = "Reinstate", page = accounts.pageNumber(), _csrf = _csrf)
                                @else
                                    @template.admin.accountAction(account = account, action = "suspend", label = "Suspend", page = accounts.pageNumber(), _csrf = _csrf)
                                @endif
                            </td>
                        </tr>
                    @endfor
                </tbody>
            </table>
        @endif

        @template.components.pager(page = accounts, baseUrl = "/admin/accounts")
    </section>
`)
