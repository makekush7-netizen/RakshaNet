import os

class Config:
    ENVIRONMENT: str = os.getenv("ENVIRONMENT", "production")
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    
    # Model Artifact Paths
    MODEL_PATH: str = os.getenv("MODEL_PATH", "models/model.pkl")
    METADATA_PATH: str = os.getenv("METADATA_PATH", "models/metadata.json")
    FEATURE_SCHEMA_PATH: str = os.getenv("FEATURE_SCHEMA_PATH", "models/feature_schema.json")
    METRICS_PATH: str = os.getenv("METRICS_PATH", "models/metrics.json")
    
    # Dataset Paths
    DATASET_PATH: str = os.getenv("DATASET_PATH", "data/kerala.csv")
    TEST_DATA_PATH: str = os.getenv("TEST_DATA_PATH", "heldout_test_data.csv")
    
    # Fallback checks
    @classmethod
    def get_existing_model_path(cls) -> str:
        if os.path.exists(cls.MODEL_PATH):
            return cls.MODEL_PATH
        if os.path.exists("model.pkl"):
            return "model.pkl"
        return cls.MODEL_PATH

    @classmethod
    def get_existing_metadata_path(cls) -> str:
        if os.path.exists(cls.METADATA_PATH):
            return cls.METADATA_PATH
        if os.path.exists("metadata.json"):
            return "metadata.json"
        return cls.METADATA_PATH
