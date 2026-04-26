package com.naturalist.chemistry.product;

class ProductEntityRepositoryMockTest implements ProductEntityRepositoryTest {
    @Override
    public ProductRepository repository() {
        return new ProductEntityRepositoryMock(db);
    }
}
