package com.naturalist.account;

class AccountRepositoryMockTest implements AccountRepositoryTest {

    @Override
    public AccountRepository.AccountEntityRepository repository() {
        return new AccountRepositoryMock(db);
    }
}
