@import com.naturalist.console.admin.AdminUsageController.Gauge
@import com.naturalist.usage.UsageAlert
@import com.naturalist.usage.UsageSnapshot
@import java.util.List

@param UsageSnapshot snapshot
@param List<UsageAlert> alerts
@param List<Gauge> gauges

@template.layout.page(title = "Usage", content = @`
    @template.admin.nav(current = "usage")
    <section class="admin-usage">
        <h1>Usage</h1>
        <p>Identification-budget consumption against the configured limits.
            A gauge turns <code>amber</code> at the warning threshold and
            <code>red</code> once the limit is reached — <code>reserve</code>
            calls stop succeeding at that point until the period rolls over.</p>

        <section class="admin-usage-gauges">
            @for(var gauge : gauges)
                <div class="admin-usage-gauge admin-usage-gauge-${gauge.state()}">
                    <span class="admin-usage-gauge-label">${gauge.label()}</span>
                    <span class="admin-usage-gauge-value">${gauge.used()} / ${gauge.limit()}</span>
                </div>
            @endfor
        </section>

        <section class="admin-usage-users">
            <h2>Per-user (today)</h2>
            @if(snapshot.users().isEmpty())
                <p><em>No reservations today.</em></p>
            @else
                <table>
                    <thead>
                        <tr>
                            <th>Naturalist</th>
                            <th>Used</th>
                            <th>Limit</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(var user : snapshot.users())
                            <tr>
                                <td>${user.naturalist()}</td>
                                <td>${user.used()}</td>
                                <td>${user.limit()}</td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            @endif
        </section>

        <section class="admin-usage-alerts">
            <h2>Active alerts</h2>
            @if(alerts.isEmpty())
                <p><em>No active alerts.</em></p>
            @else
                <ul>
                    @for(var alert : alerts)
                        <li>
                            <strong>${alert.kind()}</strong>
                            <span>${alert.scope()}</span>
                            <span>${alert.period()}</span>
                            <p>${alert.message()}</p>
                            <time>${alert.at().toString()}</time>
                        </li>
                    @endfor
                </ul>
            @endif
        </section>
    </section>
`)
