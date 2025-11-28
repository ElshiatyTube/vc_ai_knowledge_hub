"""
Embedding Service - FastAPI application for generating text embeddings
using sentence-transformers
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(title="Embedding Service", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize the model (using a lightweight model for faster inference)
MODEL_NAME = "all-MiniLM-L6-v2"
logger.info(f"Loading model: {MODEL_NAME}")

try:
    model = SentenceTransformer(MODEL_NAME)
    logger.info(f"Model loaded successfully: {MODEL_NAME}")
except Exception as e:
    logger.error(f"Failed to load model: {e}")
    raise


# Request/Response models
class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    embeddings: list[list[float]]


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy"}


@app.post("/embed", response_model=EmbedResponse)
async def embed_texts(request: EmbedRequest):
    """
    Generate embeddings for a list of texts

    Args:
        request: EmbedRequest containing list of texts to embed

    Returns:
        EmbedResponse containing the generated embeddings
    """
    try:
        if not request.texts:
            raise HTTPException(status_code=400, detail="texts list cannot be empty")

        logger.info(f"Generating embeddings for {len(request.texts)} texts")

        # Generate embeddings
        embeddings = model.encode(request.texts, convert_to_numpy=False)

        # Convert to list format
        embeddings_list = [embedding.tolist() if hasattr(embedding, 'tolist') else list(embedding)
                          for embedding in embeddings]

        logger.info(f"Successfully generated embeddings for {len(request.texts)} texts")
        return EmbedResponse(embeddings=embeddings_list)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating embeddings: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed-single")
async def embed_single(request: EmbedRequest):
    """
    Generate embedding for a single text

    Args:
        request: EmbedRequest containing a single text

    Returns:
        Dictionary with the embedding
    """
    try:
        if not request.texts or len(request.texts) == 0:
            raise HTTPException(status_code=400, detail="texts list cannot be empty")

        text = request.texts[0]
        logger.info(f"Generating embedding for single text")

        # Generate embedding
        embedding = model.encode(text, convert_to_numpy=False)
        embedding_list = embedding.tolist() if hasattr(embedding, 'tolist') else list(embedding)

        logger.info(f"Successfully generated embedding")
        return {"embedding": embedding_list}

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating embedding: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/model-info")
async def model_info():
    """Get information about the loaded model"""
    return {
        "model": MODEL_NAME,
        "dimension": model.get_sentence_embedding_dimension(),
        "max_seq_length": model.max_seq_length
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

