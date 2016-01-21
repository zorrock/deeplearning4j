package org.deeplearning4j.nn.graph.vertex;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;

public class ElementWiseVertex extends BaseGraphVertex {
    public enum Op {Add, Subtract, Product};

    private Op op;
    private int nInForwardPass;

    public ElementWiseVertex(ComputationGraph graph, String name, int vertexIndex, Op op){
        this(graph,name,vertexIndex,null,null,op);
    }

    public ElementWiseVertex(ComputationGraph graph, String name, int vertexIndex, VertexIndices[] inputVertices, VertexIndices[] outputVertices, Op op) {
        super(graph, name, vertexIndex, inputVertices, outputVertices);
        this.op = op;
    }

    @Override
    public boolean hasLayer() {
        return false;
    }

    @Override
    public boolean isOutputVertex() {
        return false;
    }

    @Override
    public Layer getLayer() {
        return null;
    }

    @Override
    public INDArray doForward(boolean training) {
        if(!canDoForward()) throw new IllegalStateException("Cannot do forward pass: inputs not set");

        nInForwardPass = inputs.length;
        if(inputs.length == 1) return inputs[0];

        switch(op){
            case Add:
                INDArray sum = inputs[0].dup();
                for( int i=1; i<inputs.length; i++){
                    sum.addi(inputs[i]);
                }
                return sum;
            case Subtract:
                if(inputs.length != 2) throw new IllegalArgumentException("ElementWise subtraction only supports 2 inputs");
                //TODO: maybe specify a convention: (first - (second + third + fourth + ...) etc?)
                //Or, maybe not. Can always build that in two steps anyway with an add and a binary subtract ops)
                return inputs[0].sub(inputs[1]);
            case Product:
                throw new UnsupportedOperationException("Not yet implemented");
            default:
                throw new UnsupportedOperationException("Unknown op: " + op);
        }
    }

    @Override
    public Pair<Gradient, INDArray[]> doBackward(boolean tbptt, int tbpttBackwardLength) {
        if(!canDoBackward()) throw new IllegalStateException("Cannot do backward pass: errors not set");

        if(nInForwardPass == 1) return new Pair<>(null,epsilons);

        switch(op){
            case Add:
                //If x=sum_i a_i then dL/da_i = dL/dx * dx/da_i = dL/dx
                INDArray[] out = new INDArray[nInForwardPass];
                out[0] = epsilons[0];
                for( int i=1; i<nInForwardPass; i++ ) out[i] = out[0].dup();
                return new Pair<>(null,out);
            case Subtract:
                INDArray[] out2 = new INDArray[2];
                out2[0] = epsilons[0];
                out2[1] = epsilons[0].mul(-1);
                return new Pair<>(null,out2);
            case Product:
                throw new UnsupportedOperationException("Not yet implemented");
            default:
                throw new UnsupportedOperationException("Unknown op: " + op);
        }
    }
}
