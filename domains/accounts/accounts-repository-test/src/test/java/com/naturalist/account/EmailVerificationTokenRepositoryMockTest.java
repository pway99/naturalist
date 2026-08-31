package com.naturalist.account;

class EmailVerificationTokenRepositoryMockTest implements EmailVerificationTokenRepositoryTest {

    @Override
    public AccountRepository.VerificationTokenRepository repository() {
        return new EmailVerificationTokenRepositoryMock(db);
    }
}
