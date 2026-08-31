package com.naturalist.account;

class AccountRepositoryMockTest implements AccountRepositoryTest {

    @Override
    public AccountRepository repository() {
        return new AccountRepositoryMock(db);
    }
}
