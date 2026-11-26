package com.naturalist.console.account;

import com.naturalist.account.AccountCommand;
import com.naturalist.account.AccountName;
import com.naturalist.naturalist.NaturalistCommand;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Purges one batch of registrations that lapsed unverified (launch item 1.5, security finding S1:
 * someone registers a victim's address, and the victim can never register it while the
 * unverified account lasts). Composes the two domains at the app root, as
 * {@link RegistrationTransaction} does, in one {@link Transactional} boundary: the accounts domain
 * deletes each lapsed account's tokens and then the account, and returns the names it actually
 * deleted; the linked {@code Naturalist} of exactly those names is then deleted. The naturalist
 * goes last, not between the tokens and the account, because only the accounts domain knows which
 * candidates were still unverified at the delete; the naturalist table holds no foreign key to the
 * account, and a failure anywhere rolls the whole batch back.
 *
 * <p>Nothing else refers to such a naturalist: member writes need the {@code VERIFIED} authority
 * ({@code SecurityConfiguration}), so a never-verified account owns no sightings, photos or notes,
 * and without VISION it cannot identify or spend budget.
 *
 * <p>Observed in {@link Observer.MonitoringMode#ALWAYS}, as registration is: {@code naturalistLinked}
 * holds when every purged account had exactly one naturalist (registration creates them together),
 * so a violation is the same data-integrity hole the login path counts under that name. The count of
 * accounts purged is the accounts command's {@code purgeLapsedRegistrations.purged} observation.
 */
@Component
class UnverifiedAccountPurgeTransaction {

    /** Accounts purged per run; a backlog drains over successive runs. */
    static final int BATCH_SIZE = 100;

    private static final Observer observer =
            Observer.forClass(UnverifiedAccountPurgeTransaction.class, Observer.MonitoringMode.ALWAYS);
    private final AccountCommand accountCommand;
    private final NaturalistCommand naturalistCommand;

    UnverifiedAccountPurgeTransaction(AccountCommand accountCommand, NaturalistCommand naturalistCommand) {
        this.accountCommand = accountCommand;
        this.naturalistCommand = naturalistCommand;
    }

    /** Purges up to {@link #BATCH_SIZE} lapsed registrations and their naturalists; returns how many accounts went. */
    @Transactional
    int purge() {
        Set<AccountName> purged = accountCommand.purgeLapsedRegistrations(BATCH_SIZE);
        if (purged.isEmpty()) {
            return 0;
        }
        int naturalists = naturalistCommand.deleteForAccounts(purged);
        observer.state("purge", i -> i.isTrue(naturalists == purged.size(), "naturalistLinked")).observe(Level.WARN);
        return purged.size();
    }
}
