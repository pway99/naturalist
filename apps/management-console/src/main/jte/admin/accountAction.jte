@import com.naturalist.account.Account

@param Account account
@param String action
@param String label
@param int page
@param org.springframework.security.web.csrf.CsrfToken _csrf = null

<form method="post" action="/admin/accounts/${account.name().value()}/${action}" class="admin-accounts-action">
    @if(_csrf != null)
        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
    @endif
    <input type="hidden" name="page" value="${page}">
    <button type="submit">${label}</button>
</form>
