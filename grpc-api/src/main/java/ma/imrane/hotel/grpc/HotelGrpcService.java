package ma.imrane.hotel.grpc;
import ma.imrane.hotel.proto.*;
import ma.imrane.hotel.data.ReservationRepository;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.function.Supplier;
@GrpcService
public class HotelGrpcService extends HotelServiceGrpc.HotelServiceImplBase {
    private final ReservationRepository repository;
    public HotelGrpcService(ReservationRepository repository){this.repository=repository;}
    @Override public void createReservation(ReservationInput input,StreamObserver<Reservation> out){reply(out,()->map(repository.create(input(input))));}
    @Override public void getReservation(Id id,StreamObserver<Reservation> out){reply(out,()->map(repository.find(id.getId())));}
    @Override public void updateReservation(UpdateRequest request,StreamObserver<Reservation> out){reply(out,()->map(repository.update(request.getId(),input(request.getInput()))));}
    @Override public void deleteReservation(Id id,StreamObserver<DeleteResponse> out){reply(out,()->DeleteResponse.newBuilder().setDeleted(repository.delete(id.getId())).build());}
    @Override public void listReservations(Empty request,StreamObserver<ReservationList> out){reply(out,()->ReservationList.newBuilder().addAllReservations(repository.list().stream().map(this::map).toList()).build());}
    private ma.imrane.hotel.model.ReservationInput input(ReservationInput input){return new ma.imrane.hotel.model.ReservationInput(input.getClientId(),input.getChambreId(),input.getDateDebut(),input.getDateFin(),input.getPreferences());}
    private Reservation map(ma.imrane.hotel.model.Reservation value){
        var client=value.getClient();var chambre=value.getChambre();
        return Reservation.newBuilder().setId(value.getId()).setClientId(value.getClientId()).setChambreId(value.getChambreId()).setDateDebut(value.getDateDebut()).setDateFin(value.getDateFin()).setPreferences(value.getPreferences()==null?"":value.getPreferences())
            .setClient(Client.newBuilder().setId(client.getId()).setNom(client.getNom()).setPrenom(client.getPrenom()).setEmail(client.getEmail()).setTelephone(client.getTelephone()))
            .setChambre(Chambre.newBuilder().setId(chambre.getId()).setType(chambre.getType()).setPrix(chambre.getPrix()).setDisponible(chambre.isDisponible())).build();
    }
    private <T> void reply(StreamObserver<T> out,Supplier<T> action){try{out.onNext(action.get());out.onCompleted();}catch(java.util.NoSuchElementException e){out.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());}catch(Exception e){out.onError(Status.INVALID_ARGUMENT.withDescription("Réservation invalide ou conflit").asRuntimeException());}}
}
